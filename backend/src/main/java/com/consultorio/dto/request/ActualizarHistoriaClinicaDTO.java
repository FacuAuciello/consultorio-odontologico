package com.consultorio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// se usa con PATCH: modifica SOLO la historia clinica y deja el resto de los datos del paciente como estaban
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActualizarHistoriaClinicaDTO {

    // puede ser vacia (el odontologo borro todo el texto), pero no null ni gigante
    @NotNull(message = "La historia clínica es obligatoria (puede ser un texto vacío)")
    @Size(max = 100000, message = "La historia clínica no puede superar los 100.000 caracteres")
    private String historiaClinica;
}
