package com.consultorio.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Base de los tests de la API. Levanta la aplicacion COMPLETA (seguridad, controllers, services y base H2 en memoria, igual que el perfil "demo")
 y manda pedidos HTTP simulados con MockMvc: asi se prueba lo que ningun test con Mockito puede probar, como el codigo HTTP que
 recibe el front, el formato del error, las validaciones de los DTO y el login con sesion.

 Todos los tests comparten la misma base, y los datos de demo (que se cargan relativos a la semana actual) ya estan adentro.
 Para que los tests no se pisen entre si ni dependan del dia en que se corran, cada uno usa su PROPIO lunes de marzo de 2031
 (diaLibre) y su propio DNI (dniNuevo): ningun dato de demo cae en esas fechas
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
abstract class ApiTestBase {

    // lunes inicial; cada llamada a diaLibre() avanza una semana
    private static final LocalDate PRIMER_LUNES = LocalDate.of(2031, 3, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    private static final AtomicInteger SEMANAS_USADAS = new AtomicInteger();
    private static final AtomicInteger DNI_SIGUIENTE = new AtomicInteger(40_000_000);

    @Autowired
    protected MockMvc mockMvc;

    // un lunes que ningun otro test usa
    protected LocalDate diaLibre() {
        return PRIMER_LUNES.plusWeeks(SEMANAS_USADAS.getAndIncrement());
    }

    // un DNI valido (8 numeros) que ningun otro test usa. Los de demo empiezan con 2 y 3
    protected String dniNuevo() {
        return String.valueOf(DNI_SIGUIENTE.getAndIncrement());
    }

    // inicia sesion con el usuario del perfil demo y devuelve la sesion, para mandarla en los pedidos siguientes (es la cookie JSESSIONID del navegador)
    protected MockHttpSession iniciarSesion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario": "admin", "contrasenia": "demo1234"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    protected String jsonPaciente(String nombre, String apellido, String dni) {
        return """
                {"nombre": "%s", "apellido": "%s", "dni": "%s", "telefono": "223 555-0100",
                 "direccion": "Calle Falsa 123", "fechaNacimiento": "1990-05-20"}
                """.formatted(nombre, apellido, dni);
    }

    // da de alta un paciente nuevo (con DNI propio) y devuelve su id
    protected long crearPaciente(MockHttpSession sesion) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/pacientes")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Ana", "Prueba", dniNuevo())))
                .andExpect(status().isCreated())
                .andReturn();

        return leerId(resultado, "$.idPaciente");
    }

    protected String jsonTurno(long idPaciente, LocalDateTime fechaHora, int duracionMinutos) {
        return """
                {"idPaciente": %d, "fechaHora": "%s", "duracionMinutos": %d}
                """.formatted(idPaciente, fechaHora, duracionMinutos);
    }

    // intenta agendar un turno y devuelve la respuesta SIN verificarla, para que cada test compruebe el codigo que espera (201, 400, 409...)
    protected ResultActions agendar(MockHttpSession sesion, long idPaciente, LocalDateTime fechaHora, int duracionMinutos) throws Exception {
        return mockMvc.perform(post("/api/turnos")
                .session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonTurno(idPaciente, fechaHora, duracionMinutos)));
    }

    // agenda un turno que tiene que salir bien y devuelve su id
    protected long agendarOk(MockHttpSession sesion, long idPaciente, LocalDateTime fechaHora, int duracionMinutos) throws Exception {
        MvcResult resultado = agendar(sesion, idPaciente, fechaHora, duracionMinutos)
                .andExpect(status().isCreated())
                .andReturn();

        return leerId(resultado, "$.idTurno");
    }

    // el JSON trae los segundos ("2031-03-10T11:00:00") y LocalDateTime.toString() los omite cuando valen cero: este formato los incluye siempre
    protected String iso(LocalDateTime fechaHora) {
        return fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // saca un numero del JSON de la respuesta (jsonPath: "$.idTurno" = el campo idTurno de la raiz)
    protected long leerId(MvcResult resultado, String ruta) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), ruta);
        return id.longValue();
    }
}
