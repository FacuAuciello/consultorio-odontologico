package com.consultorio.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/*
 Adapta nuestro Usuario al formato que entiende Spring Security (UserDetails).
 Se guarda tambien el idUsuario para poder devolverlo en /api/auth/me.
 Spring Security guarda este objeto dentro de la sesion HTTP (UserDetails ya es Serializable)
 */
public class UsuarioUserDetails implements UserDetails {

    private final Long idUsuario;
    private final String nombreUsuario;
    private final String contrasenia;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioUserDetails(Long idUsuario, String nombreUsuario, String contrasenia, boolean activo,
                              Collection<? extends GrantedAuthority> authorities) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.contrasenia = contrasenia;
        this.activo = activo;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return contrasenia;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    // si devuelve false, Spring Security rechaza el login aunque la contrasenia sea correcta
    @Override
    public boolean isEnabled() {
        return activo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
}
