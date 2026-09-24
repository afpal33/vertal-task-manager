package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.controller.AdminController;
import com.fabrizioroot.vertal.model.Dispositivo;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceRevocationTests {
    @Test
    void administratorCanRevokeLostDevice() {
        Organizacion organization = new Organizacion();
        organization.setId(1L);
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setRol(Rol.ADMINISTRADOR_SISTEMAS);
        admin.setOrganizacion(organization);
        Usuario owner = new Usuario();
        owner.setId(2L);
        owner.setOrganizacion(organization);
        Dispositivo device = new Dispositivo();
        device.setId(10L);
        device.setUsuario(owner);
        device.setActivo(true);
        VinculacionDispositivo link = new VinculacionDispositivo();
        link.setActiva(true);

        AuthorizationService authorization = mock(AuthorizationService.class);
        when(authorization.current(1L)).thenReturn(admin);
        when(authorization.isAdmin(admin)).thenReturn(true);
        DispositivoRepository devices = mock(DispositivoRepository.class);
        when(devices.findById(10L)).thenReturn(Optional.of(device));
        VinculacionDispositivoRepository links = mock(VinculacionDispositivoRepository.class);
        when(links.findByDispositivoIdAndActivaTrue(10L)).thenReturn(List.of(link));
        AdminController controller = new AdminController(
                mock(AuthService.class),
                authorization,
                mock(SolicitudVinculacionRepository.class),
                mock(UsuarioRepository.class),
                links,
                devices);

        controller.revokeDevice(1L, 10L);

        assertFalse(device.isActivo());
        assertFalse(link.isActiva());
        verify(devices).save(device);
        verify(links).save(link);
    }
}
