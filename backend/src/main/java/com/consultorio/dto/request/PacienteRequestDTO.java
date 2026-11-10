package com.consultorio.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/*
 Datos que manda el front para CREAR o EDITAR un paciente.
 Se usa un solo DTO para las dos operaciones porque los campos y las reglas son identicos
 (tener CrearPacienteDTO y ActualizarPacienteDTO iguales seria codigo duplicado)

 Las validaciones reflejan las columnas NOT NULL y los largos de la tabla: asi un dato faltante da 400 con un mensaje claro
 y no un error de base de datos (que ademas expone nombres de tablas y columnas)
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PacienteRequestDTO {

    // \p{L} = cualquier letra (incluye tildes, ñ, ü). Se permiten espacios, punto, apostrofe y guion (ej: "De la Torre", "O'Connor")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Pattern(regexp = "^\\s*$|^[\\p{L}][\\p{L} .'-]*$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 60, message = "El apellido no puede superar los 60 caracteres")
    @Pattern(regexp = "^\\s*$|^[\\p{L}][\\p{L} .'-]*$", message = "El apellido solo puede contener letras y espacios")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\s*$|^\\d{6,9}$", message = "El DNI debe tener entre 6 y 9 números, sin puntos")
    private String dni;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\s*$|^[0-9+()\\s-]{6,25}$", message = "El teléfono solo puede contener números, espacios, +, - y paréntesis")
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 150, message = "La dirección no puede superar los 150 caracteres")
    private String direccion;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
    private LocalDate fechaNacimiento;

    /*
     Validacion entre campos: un metodo isXxx con @AssertTrue
     le dice a Bean Validation "este dato es valido solo si devuelve true".
     Si la fecha es null no se queja aca: de eso se ocupa @NotNull
     */
    @AssertTrue(message = "El año de nacimiento no puede ser anterior a 1900")
    public boolean isFechaNacimientoRazonable() {
        return fechaNacimiento == null || fechaNacimiento.getYear() >= 1900;
    }
}
