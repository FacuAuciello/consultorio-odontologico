package com.consultorio.service;

import com.consultorio.dto.response.ResumenResponseDTO;
import com.consultorio.mapper.TurnoMapper;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

// numeros del panel de la agenda: turnos de hoy, de la semana, total de pacientes y el proximo turno
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumenService {

    private final ITurnoRepository turnoRepository;
    private final IPacienteRepository pacienteRepository;
    private final Clock clock;

    public ResumenResponseDTO obtenerResumen() {

        LocalDate hoy = LocalDate.now(clock);

        // la semana arranca el lunes: previousOrSame devuelve hoy mismo si hoy es lunes
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        long turnosHoy = turnoRepository.countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());

        long turnosSemana = turnoRepository.countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                lunes.atStartOfDay(), lunes.plusDays(7).atStartOfDay());

        /*
         Optional: puede que no haya ningun turno por delante. map lo convierte en DTO solo si existe;
         orElse(null) deja el campo en null en el JSON (el front muestra "sin turnos proximos")
         */
        var proximoTurno = turnoRepository
                .findFirstByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(LocalDateTime.now(clock))
                .map(TurnoMapper::aResponse)
                .orElse(null);

        return new ResumenResponseDTO(turnosHoy, turnosSemana, pacienteRepository.count(), proximoTurno);
    }
}
