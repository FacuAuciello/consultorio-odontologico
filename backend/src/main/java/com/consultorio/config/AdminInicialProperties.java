package com.consultorio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// credenciales del usuario inicial (variables ADMIN_USUARIO y ADMIN_CONTRASENIA). Vacias = no se crea ningun usuario
@ConfigurationProperties(prefix = "app.admin")
public record AdminInicialProperties(
        String usuario,
        String contrasenia
) {
}
