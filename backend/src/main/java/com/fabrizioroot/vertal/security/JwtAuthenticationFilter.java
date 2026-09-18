package com.fabrizioroot.vertal.security;

import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final UsuarioRepository usuarios;
    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarios) { this.jwtService = jwtService; this.usuarios = usuarios; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try {
            Usuario u = usuarios.findById(jwtService.userId(header.substring(7))).orElse(null);
            if (u != null && u.isActivo()) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))));
        } catch (RuntimeException ignored) { }
        chain.doFilter(request, response);
    }
}