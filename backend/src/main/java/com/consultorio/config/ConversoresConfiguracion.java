package com.consultorio.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalTime;

/*
 Le ensena a Spring a convertir el texto de application.properties ("08:00") a LocalTime al llenar AgendaProperties.
 @ConfigurationPropertiesBinding marca el converter para que se use SOLO al leer propiedades.
 El metodo es static a proposito: asi Spring lo crea bien temprano, antes de empezar a leer las propiedades.
 Es una clase con nombre y no una lambda: Spring necesita leer en la declaracion de la clase los tipos <String, LocalTime>
 para saber que conversion hace, y de una lambda no los puede deducir (fallaria al arrancar)
 */
@Configuration
public class ConversoresConfiguracion {

    @Bean
    @ConfigurationPropertiesBinding
    public static Converter<String, LocalTime> convertidorHora() {
        return new TextoALocalTime();
    }

    static class TextoALocalTime implements Converter<String, LocalTime> {

        @Override
        public LocalTime convert(String texto) {
            return LocalTime.parse(texto.trim()); // formato ISO: "HH:mm" o "HH:mm:ss"
        }
    }
}
