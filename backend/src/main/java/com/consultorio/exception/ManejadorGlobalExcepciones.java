package com.consultorio.exception;

import com.consultorio.dto.response.RespuestaErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/*
 -@RestControllerAdvice: esta clase observa las excepciones que lanzan los controllers/services y las transforma en respuestas HTTP
 -@ExceptionHandler le dice a Spring que excepcion atrapa cada metodo; Spring elige siempre el handler MAS especifico
 -ResponseEntity permite controlar la respuesta completa: body + status
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    // el detalle de los errores inesperados se registra aca (servidor) y nunca se manda al cliente
    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    // un unico metodo para construir la respuesta: evita repetir las mismas 4 lineas en cada handler
    private static ResponseEntity<RespuestaErrorDTO> responder(HttpStatus status, String mensaje) {
        return ResponseEntity
                .status(status)
                .body(RespuestaErrorDTO.de(status, mensaje));
    }

    // TODAS las excepciones de negocio (paciente no encontrado, DNI repetido, turno superpuesto...) pasan por aca:
    // cada una ya trae su propio codigo HTTP (ver ConsultorioException)
    @ExceptionHandler(ConsultorioException.class)
    public ResponseEntity<RespuestaErrorDTO> negocio(ConsultorioException ex) {
        return responder(ex.getStatus(), ex.getMessage());
    }

    // fallo una validacion del DTO (@NotBlank, @Size...). Se juntan TODOS los mensajes en uno solo, asi el usuario corrige todo de una vez
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaErrorDTO> validacion(MethodArgumentNotValidException ex) {

        String mensaje = ex.getBindingResult()
                .getFieldErrors()              // errores de campos del DTO, devuelve una lista
                .stream()
                .map(error -> error.getDefaultMessage()) // el texto que escribimos en cada anotacion
                .distinct()                    // si dos reglas del mismo campo fallan con el mismo texto, no se repite
                .collect(Collectors.joining(". "));

        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    // JSON mal formado, fecha con formato invalido, un texto donde iba un numero...
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaErrorDTO> mensajeNoLegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
    }

    // valor con tipo incorrecto en la URL (ej: /api/pacientes/abc donde se espera un id numerico, o ?desde=cualquiercosa).
    // TypeMismatchException no implementa ErrorResponse y el handler generico de abajo lo convertiria en 500: por eso se maneja aparte
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<RespuestaErrorDTO> tipoDeValorIncorrecto(TypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor enviado no es válido");
    }

    // red de seguridad ante dos requests simultaneos: si dos personas guardan el mismo DNI a la vez, ambas pasan la
    // validacion del service pero la restriccion UNIQUE de la base rechaza la segunda
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaErrorDTO> integridadDeDatos(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "Ya existe un registro con esos datos");
    }

    // credenciales incorrectas al iniciar sesion. El mensaje es fijo y no distingue "usuario inexistente" de "clave incorrecta":
    // asi no se revela que usuarios existen
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespuestaErrorDTO> credencialesInvalidas(AuthenticationException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    // va ANTES del handler generico: si no, Exception.class la atraparia primero y un 403 se convertiria en 500
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaErrorDTO> accesoDenegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto");
    }

    // red de seguridad: cualquier excepcion que ningun handler de arriba haya tomado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaErrorDTO> errorInesperado(Exception ex) {

        // las excepciones propias del framework (405 metodo incorrecto, 404 ruta inexistente, falta un parametro...)
        // ya traen su status: se respeta para no convertir errores del cliente en 500
        if (ex instanceof ErrorResponse errorFramework) {

            int codigo = errorFramework.getStatusCode().value();
            HttpStatus status = HttpStatus.resolve(codigo);

            if (codigo >= 500) {
                log.error("Error del framework no controlado", ex);
            } else {
                log.warn("Solicitud rechazada por el framework: {}", ex.getMessage());
            }

            return responder(
                    status != null ? status : HttpStatus.BAD_REQUEST,
                    codigo >= 500 ? "Ocurrió un error inesperado. Intentá de nuevo más tarde" : mensajeNeutroCliente(codigo));
        }

        // el stack completo queda en el log del servidor; el cliente solo ve un mensaje neutro (sin SQL ni nombres internos)
        log.error("Error inesperado", ex);

        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo más tarde");
    }

    // texto fijo para errores 4xx del framework: evita devolver el mensaje interno de la excepcion
    private String mensajeNeutroCliente(int codigo) {
        if (codigo == HttpStatus.NOT_FOUND.value()) {
            return "El recurso solicitado no existe";
        }
        if (codigo == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return "Método no permitido para este recurso";
        }
        return "La solicitud no es válida";
    }
}
