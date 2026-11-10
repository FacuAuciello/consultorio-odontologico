package com.consultorio.service;

import com.consultorio.config.AgendaProperties;
import com.consultorio.dto.request.TurnoRequestDTO;
import com.consultorio.entity.Paciente;
import com.consultorio.entity.Turno;
import com.consultorio.exception.*;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    // el lunes 26/10/2026: los tests usan fechas FIJAS para no depender del dia en que se corran
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    @Mock
    private ITurnoRepository turnoRepository;

    @Mock
    private IPacienteRepository pacienteRepository;

    // el service abre sus transacciones con un TransactionTemplate; en el test el gestor es un mock (no hay base), asi que ejecuta la accion y nada mas
    @Mock
    private PlatformTransactionManager gestorTransacciones;

    private TurnoService turnoService;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        // la misma configuracion que application.properties, armada a mano (AgendaProperties es un record)
        AgendaProperties agenda = new AgendaProperties(
                LocalTime.of(8, 0),
                LocalTime.of(12, 30),
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                List.of(30, 60),
                30);

        // reloj fijo: "hoy" siempre es el lunes 26/10/2026
        Clock reloj = Clock.fixed(LUNES.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        turnoService = new TurnoService(turnoRepository, pacienteRepository, agenda, reloj, new TransactionTemplate(gestorTransacciones));

        paciente = new Paciente();
        paciente.setIdPaciente(1L);
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
    }

    private TurnoRequestDTO pedido(LocalDateTime fechaHora, int duracion) {
        return new TurnoRequestDTO(fechaHora, duracion, 1L, null, null);
    }

    // ------------------------------------------------ crear ------------------------------------------------

    @Test
    void creaUnTurnoValidoYCalculaLaHoraDeFin() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = turnoService.crearTurno(pedido(LUNES.atTime(9, 0), 60));

        assertThat(respuesta.getFechaHora()).isEqualTo(LUNES.atTime(9, 0));
        assertThat(respuesta.getFechaHoraFin()).isEqualTo(LUNES.atTime(10, 0));
        assertThat(respuesta.getApellidoPaciente()).isEqualTo("Pérez");
    }

    @Test
    void conNotasYMontoLoRegistraEnLaHistoriaClinicaDelPaciente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var dto = new TurnoRequestDTO(LUNES.atTime(9, 0), 30, 1L, new BigDecimal("15000"), "Limpieza");
        turnoService.crearTurno(dto);

        assertThat(paciente.getHistoriaClinica())
                .contains("26/10/2026 09:00")
                .contains("$ 15.000")
                .contains("Limpieza");
    }

    @Test
    void sinNotasNiMontoNoTocaLaHistoriaClinica() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        turnoService.crearTurno(pedido(LUNES.atTime(9, 0), 30));

        assertThat(paciente.getHistoriaClinica()).isNull();
    }

    @Test
    void rechazaUnPacienteInexistente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(9, 0), 30)))
                .isInstanceOf(PacienteNoEncontradoException.class);
    }

    // ------------------------------------------------ reglas de la agenda ------------------------------------------------

    @Test
    void rechazaUnDiaNoLaboral() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        LocalDate sabado = LUNES.plusDays(5);

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(sabado.atTime(9, 0), 30)))
                .isInstanceOf(TurnoFueraDeAgendaException.class)
                .hasMessageContaining("sábado");

        verify(turnoRepository, never()).save(any());
    }

    @Test
    void rechazaUnTurnoQueEmpiezaAntesDeAbrir() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(7, 30), 30)))
                .isInstanceOf(TurnoFueraDeAgendaException.class);
    }

    @Test
    void rechazaUnTurnoQueTerminaDespuesDeCerrar() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        // empieza 12:00 (valido) pero dura 1 hora y terminaria 13:00, con el consultorio cerrado desde las 12:30
        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(12, 0), 60)))
                .isInstanceOf(TurnoFueraDeAgendaException.class);
    }

    @Test
    void rechazaUnHorarioQueNoCaeEnUnCasilleroDeLaAgenda() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(9, 15), 30)))
                .isInstanceOf(TurnoFueraDeAgendaException.class);
    }

    @Test
    void rechazaUnaDuracionNoPermitida() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(9, 0), 45)))
                .isInstanceOf(DuracionInvalidaException.class);
    }

    @Test
    void rechazaUnTurnoQueSePisaConOtro() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThan(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> turnoService.crearTurno(pedido(LUNES.atTime(9, 0), 30)))
                .isInstanceOf(TurnoSuperpuestoException.class);

        verify(turnoRepository, never()).save(any());
    }

    // ------------------------------------------------ editar ------------------------------------------------

    private Turno turnoExistente() {
        Turno turno = new Turno();
        turno.setIdTurno(5L);
        turno.setPaciente(paciente);
        turno.setFechaHora(LUNES.atTime(9, 0));
        turno.setFechaHoraFin(LUNES.atTime(9, 30));
        turno.setDuracionMinutos(30);
        turno.setMonto(new BigDecimal("15000.00"));
        turno.setNotasOdontologicas("Limpieza");
        return turno;
    }

    @Test
    void editarSinCambiarNotasNiMontoNoDuplicaLaHistoriaClinica() {
        when(turnoRepository.findWithPacienteByIdTurno(5L)).thenReturn(Optional.of(turnoExistente()));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        // cambia solo el horario; notas y monto (15000 con otra escala decimal) son los mismos
        var dto = new TurnoRequestDTO(LUNES.atTime(10, 0), 30, 1L, new BigDecimal("15000"), "Limpieza");
        turnoService.editarTurno(5L, dto);

        assertThat(paciente.getHistoriaClinica()).isNull();
    }

    @Test
    void editarCambiandoLasNotasAgregaUnaEntradaNueva() {
        when(turnoRepository.findWithPacienteByIdTurno(5L)).thenReturn(Optional.of(turnoExistente()));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        var dto = new TurnoRequestDTO(LUNES.atTime(9, 0), 30, 1L, new BigDecimal("15000"), "Limpieza y fluor");
        turnoService.editarTurno(5L, dto);

        assertThat(paciente.getHistoriaClinica()).contains("Limpieza y fluor");
    }

    @Test
    void editarUnTurnoInexistenteDaError() {
        when(turnoRepository.findWithPacienteByIdTurno(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.editarTurno(99L, pedido(LUNES.atTime(9, 0), 30)))
                .isInstanceOf(TurnoNoEncontradoException.class);
    }

    // ------------------------------------------------ listar y eliminar ------------------------------------------------

    @Test
    void elRangoDeFechasEsInclusivoEnAmbosExtremos() {
        turnoService.listarTurnos(LUNES, LUNES.plusDays(4));

        // desde 00:00 del lunes hasta el comienzo del sabado (exclusivo) = incluye todo el viernes
        verify(turnoRepository).findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                LUNES.atStartOfDay(), LUNES.plusDays(5).atStartOfDay());
    }

    @Test
    void eliminarUnTurnoInexistenteDaError() {
        when(turnoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> turnoService.eliminarTurno(99L))
                .isInstanceOf(TurnoNoEncontradoException.class);
    }
}
