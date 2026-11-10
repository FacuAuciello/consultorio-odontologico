package com.consultorio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/*
 Persona atendida en el consultorio.
 Se usa @Getter/@Setter y NO @Data a proposito: @Data genera equals/hashCode con todos los campos,
 y en una entidad JPA eso trae problemas (la historia clinica es enorme y el id cambia al guardar)
 */
@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Long idPaciente;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    // unique: no puede haber dos pacientes con el mismo DNI (antes el sistema lo permitia)
    @Column(nullable = false, length = 15, unique = true)
    private String dni;

    @Column(nullable = false, length = 25)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String direccion;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // texto libre que va escribiendo el odontologo. @Lob = texto largo (LONGTEXT en MySQL), sin el limite de 255 caracteres
    @Lob
    @Column(name = "historia_clinica")
    private String historiaClinica;
}
