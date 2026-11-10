package com.consultorio.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 Turno listo para mostrar. Incluye los datos MINIMOS del paciente (nombre, apellido, telefono) para que el front
 pueda dibujar la agenda y el boton de WhatsApp sin hacer una consulta extra por cada turno
 */
@Getter
@Builder
public class TurnoResponseDTO {

    private Long idTurno;
    private LocalDateTime fechaHora;
    private LocalDateTime fechaHoraFin;
    private Integer duracionMinutos;
    private BigDecimal monto;
    private String notasOdontologicas;
    private Long idPaciente;
    private String nombrePaciente;
    private String apellidoPaciente;
    private String telefonoPaciente;
}
