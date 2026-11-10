package com.consultorio.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class DuracionInvalidaException extends ConsultorioException {
    public DuracionInvalidaException(List<Integer> permitidas) {
        super(HttpStatus.BAD_REQUEST, "La duración del turno debe ser una de estas (en minutos): " + permitidas);
    }
}
