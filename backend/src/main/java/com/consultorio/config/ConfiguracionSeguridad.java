package com.consultorio.config;

import com.consultorio.security.UsuarioUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
 Toda la seguridad de la API en un solo lugar:
 1) como se guardan y verifican las contrasenias (BCrypt)
 2) como se recuerda al usuario logueado (sesion HTTP con cookie)
 3) que rutas son publicas y cuales exigen estar logueado
 4) CORS (que origen puede llamar a la API desde el navegador)
 */
@Configuration //esta clase contiene config de la app y puede declarar beans
public class ConfiguracionSeguridad {

    @Bean //le dice a spring que maneje el objeto que devuelve este metodo
    public PasswordEncoder passwordEncoder() { //interfaz a inyectar
        return new BCryptPasswordEncoder(); //implementacion BCrypt: hash con "sal" aleatoria, no reversible
    }

    /*
     Spring encuentra UsuarioUserDetailsService y lo inyecta como parametro del @Bean.
     Le entrego ese servicio (para buscar al usuario) y BCrypt como encoder (para comparar la contrasenia)
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UsuarioUserDetailsService usuarioUserDetailsService, PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(usuarioUserDetailsService);

        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        return daoAuthenticationProvider;
    }

    /*
     AuthenticationManager es el que usa AutenticacionService.login(). Spring arma el manager con el provider de arriba.
     El throws Exception es porque getAuthenticationManager() puede lanzarla y se propaga hacia quien llama
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
     Guarda y recupera "quien esta logueado" en la sesion HTTP.
     El login (AutenticacionController) lo usa para guardar al usuario, y el filtro de Spring Security
     lo usa en cada request para leer la cookie JSESSIONID y saber de quien se trata
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {

        /*
         CORS tiene que estar dentro de la cadena de seguridad y no solo en los controllers: el filtro de CORS debe correr
         ANTES que la autorizacion. El preflight OPTIONS que manda el navegador no lleva cookie y, sin esto,
         Spring Security lo rechazaria con 401 antes de llegar a cualquier controller
         */
        http.cors(Customizer.withDefaults());

        http.authorizeHttpRequests(authorize -> authorize

                // rutas publicas: iniciar y cerrar sesion (cerrar sesion tiene que funcionar aunque la sesion ya haya vencido)
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()

                // todo lo demas (pacientes, turnos, agenda, resumen...) requiere haber iniciado sesion.
                // Hoy hay un solo usuario (la secretaria); si se agregaran roles se usaria .hasRole("...")
                .anyRequest().authenticated()
        );

        // el usuario logueado se guarda en la sesion HTTP
        http.securityContext(contexto -> contexto.securityContextRepository(securityContextRepository));

        /*
         Sin sesion valida, Spring Security por defecto responderia 403. Se cambia a 401 ("no estas autenticado"),
         que es lo que el front espera para mandar al usuario a la pantalla de login
         */
        http.exceptionHandling(excepciones -> excepciones.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        /*
         CSRF (que otro sitio haga pedidos en nombre del usuario aprovechando su cookie) se evita con la cookie SameSite=Lax
         (ver application.properties): el navegador no la envia en pedidos POST/PUT/DELETE que vienen de otro sitio.
         Por eso se desactiva el mecanismo de tokens CSRF de Spring, que complica mucho una API consumida por un front aparte
         */
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }

    /*
     CORS: el navegador considera "origen distinto" a otro puerto (front 5173 vs back 8080)
     y bloquea la respuesta salvo que el back diga explicitamente que ese origen puede llamarlo.
     Solo se permiten los origenes configurados y no "*". allowCredentials(true) hace falta para que el navegador
     envie la cookie de sesion en pedidos entre origenes distintos (y exige que el origen sea explicito, nunca "*").
     Si se separan por coma se permiten varios origenes; Spring convierte el texto a List solo
     */
    @Value("${app.cors.origen-permitido}")
    private List<String> origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuracion = new CorsConfiguration();

        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Content-Type es necesario para enviar JSON
        configuracion.setAllowedHeaders(List.of("Content-Type"));
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        // la regla aplica solo a la API
        fuente.registerCorsConfiguration("/api/**", configuracion);

        return fuente;
    }
}
