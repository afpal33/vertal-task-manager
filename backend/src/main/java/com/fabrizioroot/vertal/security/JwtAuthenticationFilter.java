package com.fabrizioroot.vertal.security;

import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import com.fabrizioroot.vertal.repository.DispositivoRepository;
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
    private final JwtService jwtService; private final UsuarioRepository usuarios; private final DispositivoRepository dispositivos;
    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarios, DispositivoRepository dispositivos) { this.jwtService = jwtService; this.usuarios = usuarios; this.dispositivos = dispositivos; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try {
            String token = header.substring(7);
            String deviceId = jwtService.deviceId(token);
            String presentedDeviceId = request.getHeader("X-Device-Id");
            Usuario u = usuarios.findById(jwtService.userId(token)).orElse(null);
            boolean activeDevice = deviceId != null && deviceId.equals(presentedDeviceId) && dispositivos.findByIdentificadorDispositivoAndActivoTrue(deviceId).filter(device -> u != null && device.getUsuario() != null && device.getUsuario().getId().equals(u.getId())).isPresent();
            if (u != null && u.isActivo() && activeDevice) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))));
        } catch (RuntimeException ignored) { }
        chain.doFilter(request, response);
    }
}
