package com.consultorio.repository;

import com.consultorio.entity.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ITurnoRepository extends JpaRepository<Turno, Long> {

    /*
     @EntityGraph(attributePaths = "paciente") le dice a Hibernate que traiga el paciente en la MISMA consulta (join).
     Sin esto, al mostrar la lista haria 1 consulta por turno para buscar su paciente (el famoso problema "N+1")
     */

    // turnos dentro de un rango: desde (inclusive) hasta (exclusivo)
    @EntityGraph(attributePaths = "paciente")
    List<Turno> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = "paciente")
    List<Turno> findByPacienteIdPacienteOrderByFechaHoraDesc(Long idPaciente);

    @EntityGraph(attributePaths = "paciente")
    Optional<Turno> findFirstByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(LocalDateTime desde);

    @EntityGraph(attributePaths = "paciente")
    Optional<Turno> findWithPacienteByIdTurno(Long idTurno);

    long countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(LocalDateTime desde, LocalDateTime hasta);

    /*
     Superposicion: dos turnos se pisan cuando el que ya existe EMPIEZA antes de que termine el nuevo
     Y TERMINA despues de que empiece el nuevo. Con estas dos condiciones se cubren todos los casos
     (uno dentro del otro, solapados por el principio o por el final)
     */
    boolean existsByFechaHoraLessThanAndFechaHoraFinGreaterThan(LocalDateTime fin, LocalDateTime inicio);

    // igual que la anterior pero ignorando un turno: al EDITAR un turno no tiene que "chocar" consigo mismo
    boolean existsByFechaHoraLessThanAndFechaHoraFinGreaterThanAndIdTurnoNot(LocalDateTime fin, LocalDateTime inicio, Long idTurno);

    // al eliminar un paciente se eliminan tambien sus turnos (mismo comportamiento que la version anterior)
    void deleteByPacienteIdPaciente(Long idPaciente);
}
