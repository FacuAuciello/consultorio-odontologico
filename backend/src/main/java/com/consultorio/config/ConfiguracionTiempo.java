package com.consultorio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/*
 En vez de llamar a LocalDate.now() por todos lados, los services reciben un Clock inyectado.
 Dos ventajas: (1) "hoy" se calcula siempre en la zona horaria del consultorio aunque el servidor este en otro pais
 (los servidores en la nube suelen estar en UTC y a la noche "hoy" seria otro dia), y (2) en los tests se puede
 reemplazar por un reloj fijo para probar fechas sin depender del dia en que se corra el test
 */
@Configuration
public class ConfiguracionTiempo {

    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
