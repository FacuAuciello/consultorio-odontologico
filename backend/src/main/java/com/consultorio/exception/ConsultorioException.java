package com.consultorio.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 Clase base de TODAS las excepciones de negocio del proyecto.
 Cada excepcion hija indica con que codigo HTTP se tiene que responder (404, 409, 400...).
 Asi el manejador global necesita UN solo metodo para todas, en vez de uno por cada excepcion
 (sin esta clase habria que repetir el mismo bloque en un handler por cada excepcion)
 */
@Getter
public abstract class ConsultorioException extends RuntimeException {

    private final HttpStatus status;

    protected ConsultorioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }
}
