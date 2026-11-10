package com.consultorio.service;

import com.consultorio.config.AgendaProperties;
import com.consultorio.dto.request.TurnoRequestDTO;
import com.consultorio.dto.response.TurnoResponseDTO;
import com.consultorio.entity.Paciente;
import com.consultorio.entity.Turno;
import com.consultorio.exception.*;
import com.consultorio.mapper.TurnoMapper;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import com.consultorio.utils.HistoriaClinicaUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnoService {

    // limites usados cuando el front no manda "desde" o "hasta": equivalen a "sin limite" para una agenda real
    private static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate FECHA_MAXIMA = LocalDate.of(2999, 12, 31);

    // forLanguageTag en lugar de Locale.of(...): Locale.of recien existe desde Java 19 y el proyecto apunta a Java 17
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

    private final ITurnoRepository turnoRepository;
    private final IPacienteRepository pacienteRepository;
    private final AgendaProperties agenda;
    private final Clock clock;
    private final TransactionTemplate transaccion;

    /*
     Un unico cerrojo para toda la agenda: crear o mover un turno se hace de a uno. Sin esto, dos pedidos simultaneos para el mismo
     horario pasan los dos la validacion de superposicion (ninguno ve todavia al otro) y se guardan los dos.
     Es un cerrojo de la aplicacion, asi que protege mientras haya UN solo servidor corriendo (el caso de un consultorio).
     Con varios servidores haria falta un bloqueo en la base de datos. No lleva @RequiredArgsConstructor porque ya tiene su valor inicial
     */
    private final ReentrantLock cerrojoAgenda = new ReentrantLock();

    /*
     NOT_SUPPORTED: no abre una transaccion al entrar (la de la clase es de solo lectura). La transaccion de escritura la abre
     ejecutarDeAUno DENTRO del cerrojo, asi el commit ocurre antes de soltarlo
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TurnoResponseDTO crearTurno(TurnoRequestDTO dto) {

        return ejecutarDeAUno(() -> {

            Paciente paciente = buscarPaciente(dto.getIdPaciente());

            Turno turno = new Turno();
            aplicarDatos(turno, paciente, dto);

            Turno guardado = turnoRepository.save(turno);

            // si el turno trae notas o monto, queda registrado automaticamente en la historia clinica del paciente
            registrarEnHistoriaClinica(paciente, guardado);

            return TurnoMapper.aResponse(guardado);
        });
    }

    /*
     Turnos entre dos fechas (ambas inclusivas). Las dos son opcionales: Optional.orElse pone un valor por defecto
     cuando llega null, sin escribir un if. La agenda del calendario pide la semana visible; la tabla pide el rango que filtra el usuario
     */
    public List<TurnoResponseDTO> listarTurnos(LocalDate desde, LocalDate hasta) {

        LocalDateTime inicio = Optional.ofNullable(desde).orElse(FECHA_MINIMA).atStartOfDay();

        // "hasta" es inclusivo: se consulta hasta el comienzo del dia SIGUIENTE (limite exclusivo)
        LocalDateTime fin = Optional.ofNullable(hasta).orElse(FECHA_MAXIMA).plusDays(1).atStartOfDay();

        return turnoRepository
                .findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(inicio, fin)
                .stream()
                .map(TurnoMapper::aResponse)
                .toList();
    }

    // turnos del dia de hoy (segun la zona horaria del consultorio, ver ConfiguracionTiempo)
    public List<TurnoResponseDTO> listarTurnosDeHoy() {
        LocalDate hoy = LocalDate.now(clock);
        return listarTurnos(hoy, hoy);
    }

    public List<TurnoResponseDTO> listarTurnosPorPaciente(Long idPaciente) {

        if (!pacienteRepository.existsById(idPaciente)) {
            throw new PacienteNoEncontradoException();
        }

        return turnoRepository.findByPacienteIdPacienteOrderByFechaHoraDesc(idPaciente)
                .stream()
                .map(TurnoMapper::aResponse)
                .toList();
    }

    public TurnoResponseDTO obtenerTurnoPorId(Long idTurno) {
        return TurnoMapper.aResponse(buscarEntidad(idTurno));
    }

    // NOT_SUPPORTED y cerrojo por el mismo motivo que crearTurno: mover un turno tambien puede pisar a otro
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TurnoResponseDTO editarTurno(Long idTurno, TurnoRequestDTO dto) {

        return ejecutarDeAUno(() -> {

            Turno turno = buscarEntidad(idTurno);
            Paciente paciente = buscarPaciente(dto.getIdPaciente());

            // me guardo lo que tenia ANTES de pisarlo, para saber despues si realmente hubo cambios
            BigDecimal montoAnterior = turno.getMonto();
            String notasAnteriores = turno.getNotasOdontologicas();

            aplicarDatos(turno, paciente, dto);

            /*
             Solo se agrega una entrada nueva a la historia clinica si cambiaron las notas o el monto.
             (En la version anterior se agregaba una entrada CADA VEZ que se guardaba, aunque no se hubiera cambiado nada,
             y la historia se llenaba de entradas repetidas)
             */
            boolean cambioNotasOMonto = !Objects.equals(notasAnteriores, turno.getNotasOdontologicas())
                    || !montosIguales(montoAnterior, turno.getMonto());

            if (cambioNotasOMonto) {
                registrarEnHistoriaClinica(paciente, turno);
            }

            return TurnoMapper.aResponse(turno);
        });
    }

    @Transactional
    public void eliminarTurno(Long idTurno) {

        if (!turnoRepository.existsById(idTurno)) {
            throw new TurnoNoEncontradoException();
        }

        turnoRepository.deleteById(idTurno);
    }

    // ---- metodos privados ----

    /*
     Ejecuta la accion con el cerrojo tomado y dentro de UNA transaccion de escritura. El orden importa: la transaccion se abre
     y se cierra (commit) adentro del cerrojo, asi el siguiente pedido recien entra cuando el turno anterior ya esta guardado y lo ve.
     El unlock va en el finally para que el cerrojo se suelte aunque la accion lance una excepcion (por ej. un turno superpuesto)
     */
    private <T> T ejecutarDeAUno(Supplier<T> accion) {

        cerrojoAgenda.lock();

        try {
            return transaccion.execute(estado -> accion.get());
        } finally {
            cerrojoAgenda.unlock();
        }
    }

    private Turno buscarEntidad(Long idTurno) {
        return turnoRepository.findWithPacienteByIdTurno(idTurno)
                .orElseThrow(TurnoNoEncontradoException::new);
    }

    private Paciente buscarPaciente(Long idPaciente) {
        return pacienteRepository.findById(idPaciente)
                .orElseThrow(PacienteNoEncontradoException::new);
    }

    /*
     Valida las reglas de la agenda y copia los datos del DTO al turno. Se usa tanto al crear como al editar,
     asi las reglas son siempre las mismas. Orden: primero las reglas baratas (no consultan la base) y al final la superposicion
     */
    private void aplicarDatos(Turno turno, Paciente paciente, TurnoRequestDTO dto) {

        // se descartan segundos y nanosegundos: la agenda trabaja en minutos
        LocalDateTime inicio = dto.getFechaHora().truncatedTo(ChronoUnit.MINUTES);

        validarDuracion(dto.getDuracionMinutos());

        LocalDateTime fin = inicio.plusMinutes(dto.getDuracionMinutos());

        validarDentroDeLaAgenda(inicio, fin);
        validarSinSuperposicion(inicio, fin, turno.getIdTurno());

        turno.setFechaHora(inicio);
        turno.setFechaHoraFin(fin);
        turno.setDuracionMinutos(dto.getDuracionMinutos());
        turno.setMonto(dto.getMonto());
        turno.setNotasOdontologicas(limpiarTexto(dto.getNotasOdontologicas()));
        turno.setPaciente(paciente);
    }

    private void validarDuracion(Integer duracionMinutos) {
        if (!agenda.duracionesPermitidas().contains(duracionMinutos)) {
            throw new DuracionInvalidaException(agenda.duracionesPermitidas());
        }
    }

    private void validarDentroDeLaAgenda(LocalDateTime inicio, LocalDateTime fin) {

        // 1) dia laboral. getDisplayName arma el nombre del dia en español ("sábado")
        if (!agenda.diasLaborales().contains(inicio.getDayOfWeek())) {
            String dia = inicio.getDayOfWeek().getDisplayName(TextStyle.FULL, ARGENTINA);
            throw new TurnoFueraDeAgendaException("El consultorio no atiende el día " + dia);
        }

        // 2) horario: tiene que empezar y TERMINAR dentro del horario de atencion (y el mismo dia)
        boolean empiezaAntes = inicio.toLocalTime().isBefore(agenda.horaInicio());
        boolean terminaDespues = !fin.toLocalDate().equals(inicio.toLocalDate()) || fin.toLocalTime().isAfter(agenda.horaFin());

        if (empiezaAntes || terminaDespues) {
            throw new TurnoFueraDeAgendaException(
                    "El turno tiene que estar dentro del horario de atención (" + agenda.horaInicio() + " a " + agenda.horaFin() + ")");
        }

        // 3) los turnos arrancan en los "casilleros" de la agenda: 08:00, 08:30, 09:00... (cada intervaloMinutos desde la hora de apertura)
        long minutosDesdeApertura = Duration.between(agenda.horaInicio(), inicio.toLocalTime()).toMinutes();

        if (minutosDesdeApertura % agenda.intervaloMinutos() != 0) {
            throw new TurnoFueraDeAgendaException(
                    "Los turnos tienen que empezar cada " + agenda.intervaloMinutos() + " minutos (por ejemplo 08:00, 08:30, 09:00)");
        }
    }

    // idTurnoPropio es null al crear; al editar es el id del turno que se mueve, que no tiene que "chocar" consigo mismo
    private void validarSinSuperposicion(LocalDateTime inicio, LocalDateTime fin, Long idTurnoPropio) {

        boolean hayChoque = (idTurnoPropio == null)
                ? turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThan(fin, inicio)
                : turnoRepository.existsByFechaHoraLessThanAndFechaHoraFinGreaterThanAndIdTurnoNot(fin, inicio, idTurnoPropio);

        if (hayChoque) {
            throw new TurnoSuperpuestoException();
        }
    }

    private void registrarEnHistoriaClinica(Paciente paciente, Turno turno) {

        String historiaActualizada = HistoriaClinicaUtils.conNuevaEntrada(
                paciente.getHistoriaClinica(),
                turno.getFechaHora(),
                turno.getMonto(),
                turno.getNotasOdontologicas());

        // el paciente es una entidad gestionada: al cambiar su historia, Hibernate la guarda solo al cerrar la transaccion
        paciente.setHistoriaClinica(historiaActualizada);
    }

    // texto vacio o solo espacios -> null (en la base queda NULL en vez de un string en blanco)
    private String limpiarTexto(String texto) {
        return Optional.ofNullable(texto)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .orElse(null);
    }

    // BigDecimal.equals() considera distintos 1000 y 1000.00 (distinta escala); compareTo compara el valor numerico
    private boolean montosIguales(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }
}
