package com.consultorio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 Un turno agendado para un paciente.
 Mejoras respecto de la version con JSP:
 - fechaHora es LocalDateTime (antes java.util.Date + @Temporal)
 - la duracion es un numero de minutos (antes un String "30" que habia que parsear)
 - el monto es BigDecimal (antes un String): con dinero nunca se usa double/float
 - fechaHoraFin ahora se guarda en la base, asi se puede consultar "que turnos se pisan" directamente con SQL
 */
@Entity
@Table(name = "turno", indexes = @Index(name = "idx_turno_fecha_hora", columnList = "fecha_hora"))
@Getter
@Setter
@NoArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Long idTurno;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // se calcula en el service como fechaHora + duracionMinutos
    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // opcional: puede quedar vacio hasta que se cobre la consulta
    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Lob
    @Column(name = "notas_odontologicas")
    private String notasOdontologicas;

    /*
     LAZY: al traer un turno NO se trae el paciente completo (con su historia clinica) salvo que se necesite.
     Por eso el repositorio usa @EntityGraph en las consultas donde si se muestra el paciente
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id_paciente", nullable = false)
    private Paciente paciente;
}
