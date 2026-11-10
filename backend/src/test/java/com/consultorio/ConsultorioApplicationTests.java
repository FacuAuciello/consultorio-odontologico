package com.consultorio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/*
 Verifica que toda la aplicacion ARRANCA: que Spring pueda armar todos los beans (seguridad, JPA, repositorios...).
 Usa el perfil "demo" (base H2 en memoria), asi el test no necesita MySQL ni variables de entorno
 */
@SpringBootTest
@ActiveProfiles("demo")
class ConsultorioApplicationTests {

    @Test
    void contextLoads() {
    }
}
