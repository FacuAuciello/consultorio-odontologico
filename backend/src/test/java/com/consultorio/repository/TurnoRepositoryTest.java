package com.consultorio.repository;

import com.consultorio.entity.Paciente;
import com.consultorio.entity.Turno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 Las consultas de ITurnoRepository contra la base de verdad (H2 en memoria). Spring Data arma estas consultas a partir del NOMBRE
 del metodo, asi que un nombre mal escrito o una condicion invertida se descubre aca y no en produccion.
 @Transactional en un test: cada test se deshace al terminar, asi no queda nada guardado para el siguiente.
 Se usan fechas de 2032 para no cruzarse con los turnos de demo (que se cargan en la semana actual)
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class TurnoRepositoryTest {

    private static final LocalDate DIA = LocalDate.of(2032, 5, 10);

    @Autowired
    private ITurnoRepository turnoRepository;

    @Autowired
    private IPacienteRepository pacienteRepository;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        paciente = guardarPaciente("40111222", "Repo");
    }

    // ------------------------------------------------ superposicion ------------------------------------------------

    /*
     Hay un turno de 9:00 a 10:00. La consulta recibe el inicio y el fin del turno NUEVO y dice si se pisa con alguno.
     Se pisan cuando el existente EMPIEZA antes de que termine el nuevo Y TERMINA despues de que empiece el nuevo.
     Cada fila es: inicio del nuevo, fin del nuevo, si tiene que haber choque
     */
    @ParameterizedTest(name = "nuevo {0} a {1} -> choque: {2}")
    @CsvSource({
            "09:00, 10:00, true",   // exactamente el mismo horario
            "09:30, 10:30, true",   // pisa el final del existente
            "08:30, 09:30, true",   // pisa el principio del existente
            "09:15, 09:45, true",   // queda adentro del existente
            "08:30, 10:30, true",   // el nuevo cubre al existente
            "10:00, 10:30, false",  // empieza justo cuando el otro termina: NO se pisan
            "08:30, 09:00, false",  // termina justo cuando el otro empieza: NO se pisan
            "11:00, 11:30, false"   // lejos
    })
    void detectaLaSuperposicionEnTodosLosCasos(LocalTime inicio, LocalTime fin, boolean hayChoque) {
        guardarTurno(LocalTime.of(9, 0), 60);

        boolean resultado = turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThan(DIA.atTime(fin), DIA.atTime(inicio));

        assertThat(resultado).isEqualTo(hayChoque);
    }

    // al EDITAR un turno no tiene que chocar consigo mismo, pero si con otro
    @Test
    void ignoraAlTurnoQueSeEstaEditando() {
        Turno propio = guardarTurno(LocalTime.of(9, 0), 60);
        LocalDateTime inicio = DIA.atTime(9, 0);
        LocalDateTime fin = DIA.atTime(10, 0);

        // solo se pisa consigo mismo: no hay choque
        assertThat(turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThanAndIdTurnoNot(fin, inicio, propio.getIdTurno())).isFalse();

        // ahora hay otro turno que tambien se pisa: si hay choque
        guardarTurno(LocalTime.of(9, 30), 30);
        assertThat(turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThanAndIdTurnoNot(fin, inicio, propio.getIdTurno())).isTrue();
    }

    // ------------------------------------------------ consultas por fecha ------------------------------------------------

    // "desde" es inclusivo y "hasta" es exclusivo, y salen ordenados por hora
    @Test
    void listaPorRangoOrdenadoPorHoraConDesdeInclusivoYHastaExclusivo() {
        guardarTurno(LocalTime.of(11, 0), 30);
        guardarTurno(LocalTime.of(9, 0), 30);
        guardarTurno(LocalTime.of(10, 0), 30);

        List<Turno> turnos = turnoRepository.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                DIA.atTime(9, 0), DIA.atTime(11, 0));

        // entran las 9:00 y las 10:00; las 11:00 no entra porque "hasta" es exclusivo
        assertThat(turnos).extracting(Turno::getFechaHora)
                .containsExactly(DIA.atTime(9, 0), DIA.atTime(10, 0));
    }

    @Test
    void cuentaLosTurnosDeUnRango() {
        guardarTurno(LocalTime.of(9, 0), 30);
        guardarTurno(LocalTime.of(10, 0), 30);

        long cantidad = turnoRepository.countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay());

        assertThat(cantidad).isEqualTo(2);
    }

    @Test
    void buscaElProximoTurnoPorDelante() {
        guardarTurno(LocalTime.of(10, 0), 30);
        guardarTurno(LocalTime.of(9, 0), 30);

        var proximo = turnoRepository.findFirstByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(DIA.atTime(9, 30));

        assertThat(proximo).isPresent();
        assertThat(proximo.get().getFechaHora()).isEqualTo(DIA.atTime(10, 0));
    }

    @Test
    void elProximoTurnoPuedeNoExistir() {
        assertThat(turnoRepository.findFirstByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(LocalDateTime.of(2999, 1, 1, 0, 0))).isEmpty();
    }

    // ------------------------------------------------ por paciente ------------------------------------------------

    @Test
    void losTurnosDeUnPacienteSalenDelMasRecienteAlMasViejo() {
        guardarTurno(LocalTime.of(9, 0), 30);
        guardarTurno(LocalTime.of(11, 0), 30);

        List<Turno> turnos = turnoRepository.findByPacienteIdPacienteOrderByFechaHoraDesc(paciente.getIdPaciente());

        assertThat(turnos).extracting(Turno::getFechaHora)
                .containsExactly(DIA.atTime(11, 0), DIA.atTime(9, 0));
    }

    // al eliminar un paciente se borran sus turnos y solo los suyos
    @Test
    void borraLosTurnosDeUnPacienteSinTocarLosDeOtro() {
        guardarTurno(LocalTime.of(9, 0), 30);
        Paciente otro = guardarPaciente("40111333", "Otro");
        guardarTurnoDe(otro, LocalTime.of(10, 0), 30);

        turnoRepository.deleteByPacienteIdPaciente(paciente.getIdPaciente());

        assertThat(turnoRepository.findByPacienteIdPacienteOrderByFechaHoraDesc(paciente.getIdPaciente())).isEmpty();
        assertThat(turnoRepository.findByPacienteIdPacienteOrderByFechaHoraDesc(otro.getIdPaciente())).hasSize(1);
    }

    @Test
    void buscaUnTurnoConSuPacienteEnUnaSolaConsulta() {
        Turno guardado = guardarTurno(LocalTime.of(9, 0), 30);

        var encontrado = turnoRepository.findWithPacienteByIdTurno(guardado.getIdTurno());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getPaciente().getApellido()).isEqualTo("Repo");
    }

    // ------------------------------------------------ ayudas ------------------------------------------------

    private Paciente guardarPaciente(String dni, String apellido) {
        Paciente nuevo = new Paciente();
        nuevo.setNombre("Test");
        nuevo.setApellido(apellido);
        nuevo.setDni(dni);
        nuevo.setTelefono("223 555-0100");
        nuevo.setDireccion("Calle 1");
        nuevo.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return pacienteRepository.saveAndFlush(nuevo);
    }

    private Turno guardarTurno(LocalTime hora, int minutos) {
        return guardarTurnoDe(paciente, hora, minutos);
    }

    // saveAndFlush manda el INSERT a la base en el momento: asi las consultas siguientes ven el turno
    private Turno guardarTurnoDe(Paciente dueno, LocalTime hora, int minutos) {
        Turno turno = new Turno();
        turno.setFechaHora(DIA.atTime(hora));
        turno.setFechaHoraFin(DIA.atTime(hora).plusMinutes(minutos));
        turno.setDuracionMinutos(minutos);
        turno.setPaciente(dueno);
        return turnoRepository.saveAndFlush(turno);
    }
}
