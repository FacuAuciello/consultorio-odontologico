package com.consultorio.exception;

import org.springframework.http.HttpStatus;

public class DniYaRegistradoException extends ConsultorioException {
    public DniYaRegistradoException() {
        super(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI");
    }
}
