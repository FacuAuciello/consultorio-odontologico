package com.consultorio.exception;

import org.springframework.http.HttpStatus;

public class PacienteNoEncontradoException extends ConsultorioException {
    public PacienteNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "Paciente no encontrado");
    }
}
