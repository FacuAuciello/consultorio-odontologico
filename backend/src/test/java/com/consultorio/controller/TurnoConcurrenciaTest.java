package com.consultorio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Dos pedidos que llegan en el mismo instante para el mismo horario. Sin proteccion, los dos pasan la validacion de superposicion
 (ninguno ve todavia al otro) y se guardan los dos: dos turnos a la misma hora. TurnoService lo evita con un cerrojo que envuelve
 toda la transaccion, y estos tests lo comprueban mandando los pedidos de verdad en paralelo.
 Si alguien le saca el cerrojo al service, estos tests tienen que fallar (se probo sacandolo a mano)
 */
class TurnoConcurrenciaTest extends ApiTestBase {

    private static final int PEDIDOS_SIMULTANEOS = 12;

    @Test
    void variosPedidosParaElMismoHorarioGuardanUnSoloTurno() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();
        LocalDateTime horario = dia.atTime(9, 0);

        List<Integer> codigos = ejecutarEnParalelo(PEDIDOS_SIMULTANEOS,
                () -> agendar(sesion, idPaciente, horario, 30).andReturn().getResponse().getStatus());

        // uno solo gano el horario y todos los demas recibieron "ya hay otro turno en ese horario"
        assertThat(codigos).filteredOn(codigo -> codigo == 201).hasSize(1);
        assertThat(codigos).filteredOn(codigo -> codigo == 409).hasSize(PEDIDOS_SIMULTANEOS - 1);

        // y en la base quedo un solo turno ese dia
        mockMvc.perform(get("/api/turnos").param("desde", dia.toString()).param("hasta", dia.toString()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // el solapamiento tambien se da cuando los turnos empiezan a horas distintas: uno de 9:00 a 10:00 y otro de 9:30 a 10:00
    @Test
    void turnosQueSeSolapanSinEmpezarALaMismaHoraTampocoSePisan() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();

        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < PEDIDOS_SIMULTANEOS; i++) {
            // la mitad pide 9:00 por una hora y la otra mitad pide 9:30 por media hora: se pisan entre si
            LocalDateTime horario = (i % 2 == 0) ? dia.atTime(9, 0) : dia.atTime(9, 30);
            int duracion = (i % 2 == 0) ? 60 : 30;
            pedidos.add(() -> agendar(sesion, idPaciente, horario, duracion).andReturn().getResponse().getStatus());
        }

        List<Integer> codigos = ejecutarTodosJuntos(pedidos);

        assertThat(codigos).filteredOn(codigo -> codigo == 201).hasSize(1);

        mockMvc.perform(get("/api/turnos").param("desde", dia.toString()).param("hasta", dia.toString()).session(sesion))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // mover dos turnos distintos al mismo horario libre a la vez: tambien tiene que quedar uno solo
    @Test
    void dosTurnosQueSeMuevenAlMismoHorarioDejanSoloUnoAhi() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();
        long idUno = agendarOk(sesion, idPaciente, dia.atTime(11, 0), 30);
        long idOtro = agendarOk(sesion, idPaciente, dia.atTime(11, 30), 30);
        LocalDateTime destino = dia.atTime(9, 0);

        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < PEDIDOS_SIMULTANEOS; i++) {
            long id = (i % 2 == 0) ? idUno : idOtro;
            pedidos.add(() -> mockMvc.perform(put("/api/turnos/" + id)
                            .session(sesion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonTurno(idPaciente, destino, 30)))
                    .andReturn().getResponse().getStatus());
        }

        ejecutarTodosJuntos(pedidos);

        // de los dos turnos solo uno pudo quedarse a las 9:00 (puede ser cualquiera de los dos)
        mockMvc.perform(get("/api/turnos").param("desde", dia.toString()).param("hasta", dia.toString()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fechaHora == '" + iso(destino) + "')]", hasSize(1)));
    }

    // ------------------------------------------------ ayudas ------------------------------------------------

    // lanza la misma accion N veces a la vez
    private List<Integer> ejecutarEnParalelo(int cantidad, Callable<Integer> accion) throws Exception {
        List<Callable<Integer>> acciones = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            acciones.add(accion);
        }
        return ejecutarTodosJuntos(acciones);
    }

    /*
     Todos los hilos se frenan en la "largada" (CountDownLatch) y salen JUNTOS cuando se abre: si cada hilo arrancara apenas se crea,
     el primero terminaria antes de que empiece el segundo y no habria concurrencia de verdad
     */
    private List<Integer> ejecutarTodosJuntos(List<Callable<Integer>> acciones) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(acciones.size());
        CountDownLatch largada = new CountDownLatch(1);

        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Callable<Integer> accion : acciones) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return accion.call();
                }));
            }

            largada.countDown();

            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                codigos.add(resultado.get());
            }
            return codigos;
        } finally {
            hilos.shutdownNow();
        }
    }
}
