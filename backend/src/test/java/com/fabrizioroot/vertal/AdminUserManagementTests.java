package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.controller.AdminController;
import com.fabrizioroot.vertal.dto.AssignRoleRequestDto;
import com.fabrizioroot.vertal.dto.UserResponseDto;
import com.fabrizioroot.vertal.model.Organizacion;
import com.fabrizioroot.vertal.model.Rol;
import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.model.VinculacionDispositivo;
import com.fabrizioroot.vertal.repository.DispositivoRepository;
import com.fabrizioroot.vertal.repository.SolicitudVinculacionRepository;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import com.fabrizioroot.vertal.repository.VinculacionDispositivoRepository;
import com.fabrizioroot.vertal.service.AuthService;
import com.fabrizioroot.vertal.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserManagementTests {
    private AuthorizationService authorization;
    private UsuarioRepository users;
    private VinculacionDispositivoRepository links;
    private AdminController controller;
    private Usuario admin;
    private Usuario target;

    @BeforeEach
    void setUp() {
        Organizacion organization = new Organizacion();
        organization.setId(1L);
        admin = user(1L, "admin", Rol.ADMINISTRADOR_SISTEMAS, organization);
        target = user(2L, "ana", Rol.USUARIO_NORMAL, organization);
        authorization = mock(AuthorizationService.class);
        users = mock(UsuarioRepository.class);
        links = mock(VinculacionDispositivoRepository.class);
        when(authorization.current(1L)).thenReturn(admin);
        when(authorization.isAdmin(admin)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        controller = new AdminController(
                mock(AuthService.class),
                authorization,
                mock(SolicitudVinculacionRepository.class),
                users,
                links,
                mock(DispositivoRepository.class));
    }

    @Test
    void administratorCanAssignManagerRole() {
        UserResponseDto response = controller.role(
                1L,
                2L,
                new AssignRoleRequestDto(Rol.MANAGER));

        assertEquals(Rol.MANAGER, target.getRol());
        assertEquals(Rol.MANAGER, response.rol());
        verify(users).save(target);
    }

    @Test
    void administratorCanDeactivateUserAndActiveLinks() {
        VinculacionDispositivo link = new VinculacionDispositivo();
        link.setActiva(true);
        when(links.findByUsuarioIdAndActivaTrue(2L)).thenReturn(List.of(link));

        controller.deactivate(1L, 2L);

        assertFalse(target.isActivo());
        assertFalse(link.isActiva());
        verify(users).save(target);
        verify(links).save(link);
    }

    private Usuario user(Long id, String username, Rol role, Organizacion organization) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setNombreUsuario(username);
        user.setNombreCompleto(username);
        user.setRol(role);
        user.setOrganizacion(organization);
        user.setActivo(true);
        return user;
    }
}
