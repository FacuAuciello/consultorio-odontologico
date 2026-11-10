package com.consultorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/*
 Punto de entrada de la API.
 @ConfigurationPropertiesScan hace que Spring busque las clases marcadas con @ConfigurationProperties
 (como AgendaProperties) y las llene con los valores de application.properties
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ConsultorioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultorioApplication.class, args);
    }

}
