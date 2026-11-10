package com.consultorio.exception;

import org.springframework.http.HttpStatus;

public class TurnoSuperpuestoException extends ConsultorioException {
    public TurnoSuperpuestoException() {
        super(HttpStatus.CONFLICT, "Ya hay otro turno en ese horario");
    }
}
