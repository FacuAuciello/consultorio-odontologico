package com.consultorio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Las reglas de la agenda vistas desde afuera: que codigo HTTP y que mensaje recibe el front. Los tests de TurnoServiceTest ya prueban
 la logica con repositorios simulados; aca se prueba la aplicacion completa, con la base de verdad
 */
class TurnoControllerTest extends ApiTestBase {

    // ------------------------------------------------ alta ------------------------------------------------

    @Test
    void agendarDevuelve201ConLaHoraDeFin() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();

        agendar(sesion, idPaciente, dia.atTime(9, 0), 60)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaHoraFin").value(iso(dia.atTime(10, 0))))
                .andExpect(jsonPath("$.duracionMinutos").value(60))
                .andExpect(jsonPath("$.idPaciente").value(idPaciente))
                .andExpect(jsonPath("$.nombrePaciente").value("Ana"));
    }

    @Test
    void agendarEnElMismoHorarioDa409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDateTime horario = diaLibre().atTime(9, 0);

        agendarOk(sesion, idPaciente, horario, 30);

        agendar(sesion, idPaciente, horario, 30)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay otro turno en ese horario"));
    }

    // una hora que arranca a las 9:00 ocupa hasta las 10:00: un turno a las 9:30 se pisa, aunque no empiece a la misma hora
    @Test
    void agendarDentroDeUnTurnoMasLargoDa409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();

        agendarOk(sesion, idPaciente, dia.atTime(9, 0), 60);

        agendar(sesion, idPaciente, dia.atTime(9, 30), 30).andExpect(status().isConflict());
    }

    // un turno que termina justo cuando empieza el otro NO se pisa
    @Test
    void dosTurnosSeguidosNoSePisan() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();

        agendarOk(sesion, idPaciente, dia.atTime(9, 0), 30);
        agendarOk(sesion, idPaciente, dia.atTime(9, 30), 30);
        agendarOk(sesion, idPaciente, dia.atTime(8, 30), 30);
    }

    @Test
    void fueraDelHorarioDeAtencionDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();

        // antes de abrir
        agendar(sesion, idPaciente, dia.atTime(7, 30), 30)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("horario de atención")));

        // empieza bien pero termina despues de cerrar (cierra 12:30)
        agendar(sesion, idPaciente, dia.atTime(12, 0), 60)
                .andExpect(status().isBadRequest());

        // el ultimo turno posible: de 12:00 a 12:30
        agendar(sesion, idPaciente, dia.atTime(12, 0), 30)
                .andExpect(status().isCreated());
    }

    @Test
    void unDiaQueElConsultorioNoAtiendeDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate sabado = diaLibre().plusDays(5);

        agendar(sesion, idPaciente, sabado.atTime(9, 0), 30)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no atiende")));
    }

    @Test
    void unaDuracionNoPermitidaDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);

        agendar(sesion, idPaciente, diaLibre().atTime(9, 0), 45)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("[30, 60]")));
    }

    // los turnos empiezan en los "casilleros" de la agenda: 09:00, 09:30...
    @Test
    void unHorarioQueNoCaeEnLaGrillaDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);

        agendar(sesion, idPaciente, diaLibre().atTime(9, 15), 30)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cada 30 minutos")));
    }

    @Test
    void conUnPacienteInexistenteDa404() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        agendar(sesion, 999999L, diaLibre().atTime(9, 0), 30)
                .andExpect(status().isNotFound());
    }

    @Test
    void sinLosDatosObligatoriosDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/turnos")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("obligatoria")))
                .andExpect(jsonPath("$.message").value(containsString("seleccionar un paciente")));
    }

    @Test
    void unMontoNegativoDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);

        mockMvc.perform(post("/api/turnos")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idPaciente": %d, "fechaHora": "%s", "duracionMinutos": 30, "monto": -5}
                                """.formatted(idPaciente, diaLibre().atTime(9, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("negativo")));
    }

    // ------------------------------------------------ historia clinica ------------------------------------------------

    // un turno con notas y monto deja una entrada automatica en la historia clinica del paciente
    @Test
    void unTurnoConNotasYMontoQuedaRegistradoEnLaHistoriaClinica() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);

        mockMvc.perform(post("/api/turnos")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idPaciente": %d, "fechaHora": "%s", "duracionMinutos": 30, "monto": 15000, "notasOdontologicas": "Limpieza y control"}
                                """.formatted(idPaciente, diaLibre().atTime(9, 0))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pacientes/" + idPaciente).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historiaClinica").value(containsString("Limpieza y control")))
                .andExpect(jsonPath("$.historiaClinica").value(containsString("$ 15.000")));
    }

    // ------------------------------------------------ edicion ------------------------------------------------

    // guardar un turno sin cambiarle el horario no tiene que chocar con... si mismo
    @Test
    void editarSinCambiarElHorarioNoChocaConsigoMismo() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDateTime horario = diaLibre().atTime(9, 0);
        long idTurno = agendarOk(sesion, idPaciente, horario, 30);

        mockMvc.perform(put("/api/turnos/" + idTurno)
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idPaciente": %d, "fechaHora": "%s", "duracionMinutos": 30, "monto": 8000}
                                """.formatted(idPaciente, horario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(8000));
    }

    @Test
    void moverUnTurnoAUnHorarioOcupadoDa409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();
        agendarOk(sesion, idPaciente, dia.atTime(9, 0), 30);
        long idOtro = agendarOk(sesion, idPaciente, dia.atTime(10, 0), 30);

        mockMvc.perform(put("/api/turnos/" + idOtro)
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTurno(idPaciente, dia.atTime(9, 0), 30)))
                .andExpect(status().isConflict());
    }

    @Test
    void moverUnTurnoAUnHorarioLibreFunciona() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate dia = diaLibre();
        long idTurno = agendarOk(sesion, idPaciente, dia.atTime(9, 0), 30);

        mockMvc.perform(put("/api/turnos/" + idTurno)
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTurno(idPaciente, dia.atTime(11, 0), 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaHora").value(iso(dia.atTime(11, 0))))
                .andExpect(jsonPath("$.fechaHoraFin").value(iso(dia.atTime(12, 0))));

        // y el horario viejo quedo libre
        agendarOk(sesion, idPaciente, dia.atTime(9, 0), 30);
    }

    @Test
    void editarUnTurnoInexistenteDa404() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);

        mockMvc.perform(put("/api/turnos/999999")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTurno(idPaciente, diaLibre().atTime(9, 0), 30)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------ baja y consultas ------------------------------------------------

    @Test
    void eliminarDa204YDespuesElTurnoYaNoExiste() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        long idTurno = agendarOk(sesion, idPaciente, diaLibre().atTime(9, 0), 30);

        mockMvc.perform(delete("/api/turnos/" + idTurno).session(sesion))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/turnos/" + idTurno).session(sesion))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Turno no encontrado"));

        mockMvc.perform(delete("/api/turnos/" + idTurno).session(sesion))
                .andExpect(status().isNotFound());
    }

    // "desde" y "hasta" son inclusivos: un turno del ultimo dia del rango entra
    @Test
    void listarPorRangoDeFechasIncluyeLosDosExtremos() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDate lunes = diaLibre();
        LocalDate martes = lunes.plusDays(1);
        LocalDate miercoles = lunes.plusDays(2);

        agendarOk(sesion, idPaciente, lunes.atTime(9, 0), 30);
        agendarOk(sesion, idPaciente, martes.atTime(9, 0), 30);
        agendarOk(sesion, idPaciente, miercoles.atTime(9, 0), 30);

        mockMvc.perform(get("/api/turnos").param("desde", lunes.toString()).param("hasta", martes.toString()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/turnos").param("desde", martes.toString()).param("hasta", martes.toString()).session(sesion))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unaFechaConFormatoInvalidoDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/turnos").param("desde", "cualquier-cosa").session(sesion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor enviado no es válido"));
    }

    // ------------------------------------------------ agenda ------------------------------------------------

    @Test
    void laConfiguracionDeLaAgendaLaLeeElFront() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/agenda/configuracion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$.horaFin").value("12:30:00"))
                .andExpect(jsonPath("$.duracionesPermitidas[0]").value(30))
                .andExpect(jsonPath("$.duracionesPermitidas[1]").value(60))
                .andExpect(jsonPath("$.intervaloMinutos").value(30))
                // lunes primero, y solo los dias laborales
                .andExpect(jsonPath("$.diasLaborales[0]").value("MONDAY"))
                .andExpect(jsonPath("$.diasLaborales.length()").value(5));
    }

    @Test
    void elResumenTraLosNumerosDelPanel() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/agenda/resumen").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnosHoy").isNumber())
                .andExpect(jsonPath("$.turnosSemana").isNumber())
                .andExpect(jsonPath("$.totalPacientes").isNumber());
    }
}
