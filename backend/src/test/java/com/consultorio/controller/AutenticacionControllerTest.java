package com.consultorio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// login, logout y que las rutas de la API pidan sesion. Un 401 es lo que el front espera para mandar al usuario a la pantalla de login
class AutenticacionControllerTest extends ApiTestBase {

    private static final String LOGIN = "/api/auth/login";

    @Test
    void loginConCredencialesCorrectasDevuelveElUsuario() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"admin\", \"contrasenia\": \"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("admin"));
    }

    @Test
    void loginConClaveIncorrectaDa401() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"admin\", \"contrasenia\": \"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    // el mensaje es el mismo si el usuario no existe: asi no se revela que usuarios hay en el sistema
    @Test
    void loginConUsuarioInexistenteDaElMismoMensajeQueConClaveIncorrecta() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"nadie\", \"contrasenia\": \"demo1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void loginSinDatosDa400() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meConSesionDevuelveAlUsuarioLogueado() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("admin"));
    }

    // despues de cerrar sesion, la cookie vieja ya no sirve
    @Test
    void logoutDestruyeLaSesion() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post("/api/auth/logout").session(sesion))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isUnauthorized());
    }

    // cerrar sesion tiene que andar aunque la sesion ya haya vencido (por eso la ruta es publica)
    @Test
    void logoutSinSesionNoFalla() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void todasLasRutasDeLaApiExigenSesion() throws Exception {
        String[] rutas = {"/api/pacientes", "/api/pacientes/1", "/api/pacientes/1/turnos", "/api/turnos", "/api/turnos/hoy",
                "/api/turnos/1", "/api/agenda/configuracion", "/api/agenda/resumen"};

        for (String ruta : rutas) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().isUnauthorized());
        }
    }
}
