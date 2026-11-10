package com.consultorio.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 Datos para CREAR o EDITAR un turno.
 Las reglas que dependen de la configuracion de la agenda (horario del consultorio, duraciones permitidas,
 que no se pise con otro turno) NO se validan aca sino en TurnoService, porque necesitan consultar la base y la configuracion
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TurnoRequestDTO {

    @NotNull(message = "La fecha y hora del turno son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración tiene que ser positiva")
    private Integer duracionMinutos;

    @NotNull(message = "Tenés que seleccionar un paciente")
    private Long idPaciente;

    // opcional. @Digits limita a lo que entra en la columna DECIMAL(12,2)
    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El monto no es válido")
    private BigDecimal monto;

    @Size(max = 5000, message = "Las notas no pueden superar los 5000 caracteres")
    private String notasOdontologicas;
}
