package com.consultorio.exception;

import org.springframework.http.HttpStatus;

public class TurnoNoEncontradoException extends ConsultorioException {
    public TurnoNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "Turno no encontrado");
    }
}
