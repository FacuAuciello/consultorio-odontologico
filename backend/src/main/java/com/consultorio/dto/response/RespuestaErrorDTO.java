package com.consultorio.dto.response;

import org.springframework.http.HttpStatus;

/*
 Formato unico de TODOS los errores de la API: {status, error, message}.
 El front siempre sabe que forma tiene un error y muestra "message" tal cual
 */
public record RespuestaErrorDTO(
        int status,
        String error,
        String message
) {

    // atajo para no repetir "new RespuestaErrorDTO(status.value(), status.getReasonPhrase(), mensaje)" en cada handler
    public static RespuestaErrorDTO de(HttpStatus status, String mensaje) {
        return new RespuestaErrorDTO(status.value(), status.getReasonPhrase(), mensaje);
    }
}
