package com.consultorio.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.consultorio.entity.Usuario;
import com.consultorio.repository.IUsuarioRepository;

import java.time.LocalDateTime;

/*
 Como la API no tiene registro publico (solo el consultorio entra), el primer usuario se crea al arrancar
 a partir de las variables ADMIN_USUARIO y ADMIN_CONTRASENIA.
 ApplicationRunner = Spring ejecuta run() una vez, justo cuando termina de arrancar la aplicacion.
 @Slf4j (Lombok) genera el logger "log" sin declararlo a mano
 */
@Component
@Order(1) // corre antes que cualquier otro runner (por ej. los datos de demo)
@RequiredArgsConstructor
@Slf4j
public class CreadorUsuarioInicial implements ApplicationRunner {

    private final AdminInicialProperties admin;
    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        boolean faltanDatos = admin.usuario() == null || admin.usuario().isBlank()
                || admin.contrasenia() == null || admin.contrasenia().isBlank();

        if (faltanDatos) {
            log.warn("No se creó ningún usuario inicial: definí ADMIN_USUARIO y ADMIN_CONTRASENIA para poder iniciar sesión");
            return;
        }

        if (usuarioRepository.existsByNombreUsuario(admin.usuario())) {
            return; // ya existe: no se pisa ni se le cambia la contrasenia
        }

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(admin.usuario().trim());
        // se guarda el HASH BCrypt, nunca la contrasenia real
        usuario.setContrasenia(passwordEncoder.encode(admin.contrasenia()));
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());

        usuarioRepository.save(usuario);

        log.info("Usuario inicial '{}' creado", usuario.getNombreUsuario());
    }
}
