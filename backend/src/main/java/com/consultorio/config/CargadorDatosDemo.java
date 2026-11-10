package com.consultorio.config;

import com.consultorio.entity.Paciente;
import com.consultorio.entity.Turno;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import com.consultorio.utils.HistoriaClinicaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/*
 Carga pacientes y turnos de EJEMPLO (todos ficticios) para poder probar el proyecto apenas se levanta.
 @Profile("demo"): esta clase existe SOLO si se arranca con el perfil "demo"; en produccion Spring ni la crea.
 Los turnos se calculan a partir de la semana actual, asi el calendario siempre muestra datos "de esta semana"
 */
@Component
@Profile("demo")
@Order(2) // despues de CreadorUsuarioInicial
@RequiredArgsConstructor
@Slf4j
public class CargadorDatosDemo implements ApplicationRunner {

    // record local para describir cada turno de ejemplo en una sola linea (mas legible que 10 setters seguidos)
    private record TurnoDemo(int semana, DayOfWeek dia, String hora, int duracion, int paciente, Integer monto, String notas) {
    }

    private final IPacienteRepository pacienteRepository;
    private final ITurnoRepository turnoRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (pacienteRepository.count() > 0) {
            return; // ya hay datos: no se duplican
        }

        List<Paciente> pacientes = pacienteRepository.saveAll(crearPacientes());

        LocalDate hoy = LocalDate.now(clock);
        LocalDate lunesDeEstaSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // semana: -1 = semana pasada, 0 = esta semana, 1 = la que viene
        List<TurnoDemo> turnosDemo = List.of(
                new TurnoDemo(-1, DayOfWeek.MONDAY, "08:30", 30, 0, 15000, "Control general. Se detecta caries leve en pieza 36."),
                new TurnoDemo(-1, DayOfWeek.TUESDAY, "10:00", 60, 3, 42000, "Endodoncia pieza 46, primera sesión."),
                new TurnoDemo(-1, DayOfWeek.WEDNESDAY, "09:00", 30, 5, 12000, "Limpieza y control."),
                new TurnoDemo(-1, DayOfWeek.THURSDAY, "11:00", 30, 7, 18000, "Restauración con resina en pieza 24."),
                new TurnoDemo(-1, DayOfWeek.FRIDAY, "08:00", 60, 1, 55000, "Extracción de cordal inferior derecho. Indicaciones postoperatorias entregadas."),

                new TurnoDemo(0, DayOfWeek.MONDAY, "08:30", 30, 2, 14000, "Control de ortodoncia."),
                new TurnoDemo(0, DayOfWeek.MONDAY, "10:00", 60, 3, 42000, "Endodoncia pieza 46, segunda sesión y obturación."),
                new TurnoDemo(0, DayOfWeek.TUESDAY, "09:00", 30, 4, null, null),
                new TurnoDemo(0, DayOfWeek.TUESDAY, "11:00", 30, 8, null, null),
                new TurnoDemo(0, DayOfWeek.WEDNESDAY, "08:00", 60, 6, null, "Blanqueamiento dental, primera sesión."),
                new TurnoDemo(0, DayOfWeek.WEDNESDAY, "11:30", 30, 0, null, null),
                new TurnoDemo(0, DayOfWeek.THURSDAY, "09:30", 30, 9, null, null),
                new TurnoDemo(0, DayOfWeek.THURSDAY, "10:30", 60, 1, null, "Control post extracción y retiro de puntos."),
                new TurnoDemo(0, DayOfWeek.FRIDAY, "08:30", 30, 5, null, null),
                new TurnoDemo(0, DayOfWeek.FRIDAY, "10:00", 30, 7, null, null),

                new TurnoDemo(1, DayOfWeek.MONDAY, "09:00", 30, 4, null, null),
                new TurnoDemo(1, DayOfWeek.TUESDAY, "10:30", 60, 6, null, "Blanqueamiento dental, segunda sesión."),
                new TurnoDemo(1, DayOfWeek.WEDNESDAY, "08:30", 30, 2, null, null),
                new TurnoDemo(1, DayOfWeek.THURSDAY, "11:30", 30, 8, null, null),
                new TurnoDemo(1, DayOfWeek.FRIDAY, "09:00", 60, 9, null, null)
        );

        LocalDateTime ahora = LocalDateTime.now(clock);

        // forEach con una lambda: por cada turno de ejemplo arma la entidad y la guarda
        turnosDemo.forEach(demo -> {

            LocalDateTime inicio = lunesDeEstaSemana
                    .plusWeeks(demo.semana())
                    .with(TemporalAdjusters.nextOrSame(demo.dia()))
                    .atTime(LocalTime.parse(demo.hora()));

            Paciente paciente = pacientes.get(demo.paciente());

            Turno turno = new Turno();
            turno.setPaciente(paciente);
            turno.setFechaHora(inicio);
            turno.setDuracionMinutos(demo.duracion());
            turno.setFechaHoraFin(inicio.plusMinutes(demo.duracion()));
            turno.setMonto(demo.monto() == null ? null : BigDecimal.valueOf(demo.monto()));
            turno.setNotasOdontologicas(demo.notas());
            turnoRepository.save(turno);

            // los turnos que ya pasaron quedan registrados en la historia clinica, igual que pasaria en el uso real
            if (inicio.isBefore(ahora)) {
                paciente.setHistoriaClinica(HistoriaClinicaUtils.conNuevaEntrada(
                        paciente.getHistoriaClinica(), inicio, turno.getMonto(), turno.getNotasOdontologicas()));
            }
        });

        log.info("Datos de demo cargados: {} pacientes y {} turnos", pacientes.size(), turnosDemo.size());
    }

    private List<Paciente> crearPacientes() {
        return List.of(
                paciente("Lucía", "Fernández", "28456123", "223 555-0101", "Av. Colón 1234", LocalDate.of(1990, 4, 12)),
                paciente("Martín", "Gómez", "31220987", "223 555-0102", "Güemes 2450", LocalDate.of(1985, 9, 30)),
                paciente("Carolina", "Benítez", "35120456", "223 555-0103", "Rivadavia 3120", LocalDate.of(1993, 1, 22)),
                paciente("Javier", "Ledesma", "27654321", "223 555-0104", "San Martín 1850", LocalDate.of(1982, 11, 5)),
                paciente("Sofía", "Martínez", "40123789", "223 555-0105", "Independencia 960", LocalDate.of(2001, 7, 18)),
                paciente("Rodrigo", "Paz", "33987654", "223 555-0106", "Alberti 4410", LocalDate.of(1988, 3, 9)),
                paciente("Valentina", "Suárez", "42111222", "223 555-0107", "Luro 2300", LocalDate.of(2004, 12, 2)),
                paciente("Esteban", "Quiroga", "22345678", "223 555-0108", "Belgrano 1575", LocalDate.of(1975, 6, 27)),
                paciente("Florencia", "Ríos", "38765432", "223 555-0109", "Mitre 3680", LocalDate.of(1996, 10, 14)),
                paciente("Nicolás", "Herrera", "36543210", "223 555-0110", "Jujuy 745", LocalDate.of(1992, 2, 25))
        );
    }

    private Paciente paciente(String nombre, String apellido, String dni, String telefono, String direccion, LocalDate nacimiento) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setDni(dni);
        p.setTelefono(telefono);
        p.setDireccion(direccion + ", Mar del Plata");
        p.setFechaNacimiento(nacimiento);
        return p;
    }
}
