package com.consultorio.controller;

import com.consultorio.dto.request.LoginRequestDTO;
import com.consultorio.dto.response.UsuarioResponseDTO;
import com.consultorio.security.UsuarioUserDetails;
import com.consultorio.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 Los controllers SOLO reciben el request, delegan en el service y devuelven la respuesta HTTP: no tienen logica de negocio.
 @RequestMapping define el prefijo de todas las rutas de la clase.

 Como funciona la sesion: al hacer login se guarda al usuario en la sesion HTTP del servidor y el navegador recibe
 una cookie (JSESSIONID). En cada pedido siguiente el navegador envia la cookie sola y Spring Security reconoce al usuario.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;
    private final SecurityContextRepository securityContextRepository;

    // POST /api/auth/login  (ruta publica)
    // @Valid hace que Spring ejecute las validaciones del DTO ANTES de entrar al metodo; si fallan responde 400
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {

        // si las credenciales son incorrectas esta linea lanza AuthenticationException y responde 401
        Authentication authentication = autenticacionService.autenticar(request);

        // si ya habia una sesion abierta se le cambia el id: evita que alguien que conocia el id viejo siga dentro (session fixation)
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        // se guarda al usuario en la sesion (la crea si no existia). Desde aca los pedidos con esa cookie quedan autenticados
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(authentication);
        securityContextRepository.saveContext(contexto, httpRequest, httpResponse);

        return ResponseEntity.ok(autenticacionService.usuarioActual((UsuarioUserDetails) authentication.getPrincipal()));
    }

    // POST /api/auth/logout  (ruta publica: tiene que andar aunque la sesion ya haya vencido)
    // Destruye la sesion del servidor; la cookie que queda en el navegador deja de servir
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
        return ResponseEntity.noContent().build();
    }

    // GET /api/auth/me -> quien esta logueado. Spring inyecta el usuario de la sesion con @AuthenticationPrincipal.
    // Si no hay sesion valida, Spring Security responde 401 antes de llegar aca
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> usuarioActual(@AuthenticationPrincipal UsuarioUserDetails usuario) {
        return ResponseEntity.ok(autenticacionService.usuarioActual(usuario));
    }
}
