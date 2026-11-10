package com.consultorio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/*
 Reglas de la agenda, leidas de application.properties (prefijo "consultorio.agenda").
 Es un record: Spring llena el constructor con los valores del archivo y despues no se puede modificar.
 Si manana el consultorio atiende tambien por la tarde, se cambia la propiedad y no el codigo
 */
@ConfigurationProperties(prefix = "consultorio.agenda")
public record AgendaProperties(
        LocalTime horaInicio,
        LocalTime horaFin,
        Set<DayOfWeek> diasLaborales,
        List<Integer> duracionesPermitidas,
        int intervaloMinutos
) {
}
