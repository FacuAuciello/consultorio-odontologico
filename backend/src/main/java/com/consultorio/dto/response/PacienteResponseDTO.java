package com.consultorio.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// detalle completo de UN paciente (incluye la historia clinica)
@Getter
@Builder
public class PacienteResponseDTO {

    private Long idPaciente;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String direccion;
    private LocalDate fechaNacimiento;
    private String historiaClinica;
}
