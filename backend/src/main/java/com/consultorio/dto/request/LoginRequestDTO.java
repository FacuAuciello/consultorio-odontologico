package com.consultorio.dto.request;

import jakarta.validation.constraints.NotBlank;

// record: clase inmutable con constructor, getters (usuario(), contrasenia()), equals y toString ya generados
public record LoginRequestDTO(

        @NotBlank(message = "El usuario es obligatorio")
        String usuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasenia

) {
}
