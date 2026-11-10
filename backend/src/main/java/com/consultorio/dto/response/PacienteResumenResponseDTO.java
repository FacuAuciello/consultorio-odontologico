package com.consultorio.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/*
 Version "liviana" del paciente para las LISTAS: no incluye la historia clinica, que puede ser muy larga
 y no se muestra en la tabla. La historia solo viaja en PacienteResponseDTO (detalle de un paciente)
 @Builder permite construirlo asi: PacienteResumenResponseDTO.builder().nombre("Ana").dni("123").build()
 */
@Getter
@Builder
public class PacienteResumenResponseDTO {

    private Long idPaciente;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String direccion;
    private LocalDate fechaNacimiento;
}
