package com.consultorio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 Persona que puede iniciar sesion en el sistema (la secretaria o el odontologo).
 Antes la contrasenia se guardaba en texto plano; ahora se guarda el hash BCrypt, que no se puede "des-hashear"
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre_usuario", nullable = false, length = 50, unique = true)
    private String nombreUsuario;

    // hash BCrypt (60 caracteres); nunca la contrasenia real
    @Column(nullable = false, length = 100)
    private String contrasenia;

    // un usuario desactivado no puede iniciar sesion, pero se conserva en la base
    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
