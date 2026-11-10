package com.consultorio.service;

import com.consultorio.dto.request.LoginRequestDTO;
import com.consultorio.dto.response.UsuarioResponseDTO;
import com.consultorio.security.UsuarioUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final AuthenticationManager authenticationManager;

    /*
     Verifica usuario y contrasenia. Devuelve el Authentication ya autenticado; guardarlo en la sesion
     (para que el usuario siga logueado en los proximos pedidos) es tarea del controller, que es quien tiene el request HTTP
     */
    public Authentication autenticar(LoginRequestDTO request) {

        /*
         Crear un objeto de autenticacion con las credenciales que llegaron del cliente.
         Todavia NO significa que el usuario este autenticado: es solo "estos son los datos que me dieron"
         */
        UsernamePasswordAuthenticationToken credenciales =
                new UsernamePasswordAuthenticationToken(request.usuario(), request.contrasenia());

        /*
         AuthenticationManager intenta autenticar. Internamente usa:
         DaoAuthenticationProvider -> UsuarioUserDetailsService (busca el usuario) + BCrypt (compara la contrasenia)
         Si algo falla lanza AuthenticationException y el ManejadorGlobalExcepciones responde 401
         */
        return authenticationManager.authenticate(credenciales);
    }

    // datos del usuario logueado (el front lo usa para mostrar quien es y para saber si la sesion sigue abierta al recargar la pagina)
    public UsuarioResponseDTO usuarioActual(UsuarioUserDetails usuario) {
        return new UsuarioResponseDTO(usuario.getIdUsuario(), usuario.getUsername());
    }
}
