package com.consultorio.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

// lo que el front necesita saber para dibujar el calendario con el horario real del consultorio
public record AgendaConfiguracionDTO(
        LocalTime horaInicio,
        LocalTime horaFin,
        List<DayOfWeek> diasLaborales,
        List<Integer> duracionesPermitidas,
        int intervaloMinutos
) {
}
