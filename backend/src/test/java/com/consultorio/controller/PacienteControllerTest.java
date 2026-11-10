package com.consultorio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// lo que ve el front al trabajar con pacientes: codigos HTTP, mensajes de error y validaciones de los DTO
class PacienteControllerTest extends ApiTestBase {

    // ------------------------------------------------ alta ------------------------------------------------

    @Test
    void crearDevuelve201ConLaUbicacionDelPacienteNuevo() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Lucía", "Gómez", dniNuevo())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pacientes/")))
                .andExpect(jsonPath("$.nombre").value("Lucía"))
                .andExpect(jsonPath("$.idPaciente").isNumber());
    }

    // los espacios del final se sacan al guardar (el mapper hace trim). Al principio no se aceptan: el @Pattern pide que el nombre arranque con una letra
    @Test
    void crearQuitaLosEspaciosSobrantesDelFinal() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Marta  ", "Díaz ", dniNuevo())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Marta"))
                .andExpect(jsonPath("$.apellido").value("Díaz"));
    }

    @Test
    void conUnDniYaRegistradoDa409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        String dni = dniNuevo();

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Ana", "Uno", dni)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Beto", "Dos", dni)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un paciente con ese DNI"));
    }

    // con los campos vacios sale UN mensaje por campo ("obligatorio") y no ademas el de formato
    @Test
    void conCamposVaciosDa400ConUnSoloMensajePorCampo() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "", "apellido": "", "dni": "", "telefono": "", "direccion": "x", "fechaNacimiento": "1990-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("El nombre es obligatorio")))
                .andExpect(jsonPath("$.message").value(containsString("El apellido es obligatorio")))
                .andExpect(jsonPath("$.message").value(containsString("El DNI es obligatorio")))
                .andExpect(jsonPath("$.message").value(containsString("El teléfono es obligatorio")))
                .andExpect(jsonPath("$.message").value(not(containsString("solo puede contener"))))
                .andExpect(jsonPath("$.message").value(not(containsString("entre 6 y 9"))));
    }

    @Test
    void conValoresConFormatoInvalidoDa400ConElMensajeDeCadaUno() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes")
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana1", "apellido": "Pérez", "dni": "12.345", "telefono": "abc",
                                 "direccion": "x", "fechaNacimiento": "1990-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("El nombre solo puede contener letras y espacios")))
                .andExpect(jsonPath("$.message").value(containsString("El DNI debe tener entre 6 y 9 números")))
                .andExpect(jsonPath("$.message").value(containsString("El teléfono solo puede contener números")));
    }

    @Test
    void conUnaFechaDeNacimientoFuturaODemasiadoAntiguaDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "apellido": "Pérez", "dni": "%s", "telefono": "223 555-0100",
                                 "direccion": "x", "fechaNacimiento": "2999-01-01"}
                                """.formatted(dniNuevo())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("anterior a hoy")));

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "apellido": "Pérez", "dni": "%s", "telefono": "223 555-0100",
                                 "direccion": "x", "fechaNacimiento": "1850-01-01"}
                                """.formatted(dniNuevo())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("1900")));
    }

    @Test
    void conUnJsonRotoDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Los datos enviados no son válidos"));
    }

    // ------------------------------------------------ consulta ------------------------------------------------

    @Test
    void obtenerUnPacienteInexistenteDa404() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/pacientes/999999").session(sesion))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paciente no encontrado"));
    }

    // el id viene en la URL: un texto donde va un numero es un error del cliente (400) y no un error del servidor (500)
    @Test
    void conUnIdQueNoEsNumericoDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/pacientes/abc").session(sesion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor enviado no es válido"));
    }

    @Test
    void buscarPorDniNombreOApellidoSinImportarMayusculas() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        String dni = dniNuevo();

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Zenobia", "Quintanilla", dni)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pacientes").param("busqueda", "quintan").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value(dni));

        mockMvc.perform(get("/api/pacientes").param("busqueda", "ZENOBIA").session(sesion))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/pacientes").param("busqueda", dni).session(sesion))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/pacientes").param("busqueda", "nadie-se-llama-asi").session(sesion))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // el listado es un resumen: no trae la historia clinica (puede ser enorme)
    @Test
    void elListadoNoIncluyeLaHistoriaClinica() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/pacientes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historiaClinica").doesNotExist());
    }

    // ------------------------------------------------ edicion ------------------------------------------------

    @Test
    void editarPuedeConservarSuPropioDniPeroNoUsarElDeOtro() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        String dniDeAna = dniNuevo();
        String dniDeBeto = dniNuevo();

        mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Ana", "Uno", dniDeAna)))
                .andExpect(status().isCreated());

        long idBeto = leerId(mockMvc.perform(post("/api/pacientes").session(sesion).contentType(MediaType.APPLICATION_JSON)
                .content(jsonPaciente("Beto", "Dos", dniDeBeto))).andReturn(), "$.idPaciente");

        // el mismo DNI de siempre: valido (no choca consigo mismo)
        mockMvc.perform(put("/api/pacientes/" + idBeto).session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Beto", "Cambiado", dniDeBeto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Cambiado"));

        // el DNI de Ana: invalido
        mockMvc.perform(put("/api/pacientes/" + idBeto).session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Beto", "Cambiado", dniDeAna)))
                .andExpect(status().isConflict());
    }

    @Test
    void editarUnPacienteInexistenteDa404() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(put("/api/pacientes/999999").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPaciente("Ana", "Uno", dniNuevo())))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------ historia clinica ------------------------------------------------

    @Test
    void laHistoriaClinicaSeActualizaConPatchYAceptaTextoVacio() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long id = crearPaciente(sesion);

        mockMvc.perform(patch("/api/pacientes/" + id + "/historia-clinica").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"historiaClinica\": \"Alergia a la penicilina\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historiaClinica").value("Alergia a la penicilina"))
                // PATCH modifica solo la historia: el resto de los datos queda como estaba
                .andExpect(jsonPath("$.nombre").value("Ana"));

        // el odontologo borro todo el texto: es valido
        mockMvc.perform(patch("/api/pacientes/" + id + "/historia-clinica").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"historiaClinica\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historiaClinica").value(""));
    }

    // sin el campo NO es lo mismo que vacio: antes dejaba la historia en null sin avisar
    @Test
    void laHistoriaClinicaSinElCampoDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long id = crearPaciente(sesion);

        mockMvc.perform(patch("/api/pacientes/" + id + "/historia-clinica").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("obligatoria")));
    }

    @Test
    void laHistoriaClinicaMuyLargaDa400() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long id = crearPaciente(sesion);
        String textoGigante = "a".repeat(100_001);

        mockMvc.perform(patch("/api/pacientes/" + id + "/historia-clinica").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"historiaClinica\": \"" + textoGigante + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("100.000")));
    }

    // ------------------------------------------------ baja ------------------------------------------------

    // al eliminar un paciente se eliminan tambien sus turnos (lo avisa el front antes de confirmar)
    @Test
    void eliminarUnPacienteBorraTambienSusTurnos() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        long idTurno = agendarOk(sesion, idPaciente, diaLibre().atTime(9, 0), 30);

        mockMvc.perform(delete("/api/pacientes/" + idPaciente).session(sesion))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pacientes/" + idPaciente).session(sesion))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/turnos/" + idTurno).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnPacienteInexistenteDa404() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(delete("/api/pacientes/999999").session(sesion))
                .andExpect(status().isNotFound());
    }

    // los turnos de un paciente salen del mas reciente al mas viejo
    @Test
    void losTurnosDelPacienteSalenDelMasRecienteAlMasViejo() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        long idPaciente = crearPaciente(sesion);
        LocalDateTime primero = diaLibre().atTime(9, 0);
        LocalDateTime segundo = diaLibre().atTime(10, 0);

        agendarOk(sesion, idPaciente, primero, 30);
        agendarOk(sesion, idPaciente, segundo, 30);

        mockMvc.perform(get("/api/pacientes/" + idPaciente + "/turnos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fechaHora").value(iso(segundo)))
                .andExpect(jsonPath("$[1].fechaHora").value(iso(primero)));
    }
}
