package com.consultorio.security;

import com.consultorio.entity.Usuario;
import com.consultorio.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 Spring Security llama a este servicio durante el login para buscar al usuario en la base.
 @RequiredArgsConstructor (Lombok) genera el constructor con todos los campos final: es la inyeccion por constructor
 de siempre, sin escribirlo a mano
 */
@Service
@RequiredArgsConstructor
public class UsuarioUserDetailsService implements UserDetailsService {

    private final IUsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // hoy hay un unico rol; la authority queda en la sesion por si manana se agregan perfiles (por ej. secretaria y odontologo)
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));

        return new UsuarioUserDetails(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getContrasenia(),
                Boolean.TRUE.equals(usuario.getActivo()),
                authorities
        );
    }
}
