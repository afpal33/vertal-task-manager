package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.dto.*;
import com.fabrizioroot.vertal.model.*;
import com.fabrizioroot.vertal.repository.*;
import com.fabrizioroot.vertal.security.JwtService;
import com.fabrizioroot.vertal.service.AuthService;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTests {
    @Test void linkingRequestIsPending() {
        OrganizacionRepository organizations = mock(OrganizacionRepository.class); when(organizations.findAll()).thenReturn(java.util.List.of(new Organizacion()));
        SolicitudVinculacionRepository requests = mock(SolicitudVinculacionRepository.class); when(requests.save(any())).thenAnswer(invocation -> { SolicitudVinculacion value=invocation.getArgument(0); value.setId(1L); return value; });
        AuthService service = new AuthService(organizations, requests, mock(UsuarioRepository.class), mock(DispositivoRepository.class), mock(VinculacionDispositivoRepository.class), mock(JwtService.class), "server-key", "bootstrap-token");
        LinkingResponseDto response = service.request(new LinkingRequestDto("ana", "phone", "device-1", "public-key"));
        assertEquals(EstadoSolicitud.PENDIENTE, response.estado()); verify(requests).save(any());
    }

    @Test void challengeLoginUsesPublicKeyAndConsumesChallenge() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); KeyPair pair=generator.generateKeyPair();
        String publicKey=Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        Dispositivo device=new Dispositivo(); device.setIdentificadorDispositivo("device-1"); device.setClavePublicaDispositivo(publicKey); device.setActivo(true); Usuario user=new Usuario(); user.setId(7L); user.setActivo(true); device.setUsuario(user);
        DispositivoRepository devices=mock(DispositivoRepository.class); when(devices.findByIdentificadorDispositivoAndActivoTrue("device-1")).thenReturn(Optional.of(device));
        JwtService jwt=mock(JwtService.class); when(jwt.createToken(7L)).thenReturn("token");
        AuthService service=new AuthService(mock(OrganizacionRepository.class),mock(SolicitudVinculacionRepository.class),mock(UsuarioRepository.class),devices,mock(VinculacionDispositivoRepository.class),jwt,"server-key", "bootstrap-token");
        ChallengeResponseDto challenge=service.challenge(new ChallengeRequestDto("device-1")); Signature signature=Signature.getInstance("SHA256withRSA"); signature.initSign(pair.getPrivate()); signature.update(challenge.challenge().getBytes()); String signed=Base64.getEncoder().encodeToString(signature.sign());
        assertEquals("token",service.login(new LoginRequestDto(challenge.challengeId(),"device-1",signed)).token());
        assertThrows(RuntimeException.class,()->service.login(new LoginRequestDto(challenge.challengeId(),"device-1",signed)));
    }

    @Test void approvalCreatesActiveUserAndDeviceLink() {
        Organizacion organization = new Organizacion(); organization.setId(1L);
        SolicitudVinculacion request = new SolicitudVinculacion(); request.setId(4L); request.setEstado(EstadoSolicitud.PENDIENTE); request.setNombreUsuarioSolicitado("ana"); request.setNombreDispositivo("phone"); request.setIdentificadorDispositivo("device-4"); request.setClavePublicaDispositivo("public-key"); request.setOrganizacion(organization);
        UsuarioRepository users = mock(UsuarioRepository.class); when(users.findByNombreUsuario("ana")).thenReturn(Optional.empty()); when(users.save(any(Usuario.class))).thenAnswer(invocation -> { Usuario user = invocation.getArgument(0); user.setId(9L); return user; });
        SolicitudVinculacionRepository requests = mock(SolicitudVinculacionRepository.class); when(requests.findById(4L)).thenReturn(Optional.of(request)); when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DispositivoRepository devices = mock(DispositivoRepository.class); when(devices.findByIdentificadorDispositivoAndActivoTrue("device-4")).thenReturn(Optional.empty()); when(devices.save(any(Dispositivo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VinculacionDispositivoRepository links = mock(VinculacionDispositivoRepository.class);
        AuthService service = new AuthService(mock(OrganizacionRepository.class), requests, users, devices, links, mock(JwtService.class), "server-key", "bootstrap-token");

        LinkingResponseDto response = service.approve(4L, user(1L, Rol.ADMINISTRADOR_SISTEMAS), new ApproveLinkingRequestDto("Ana User", null));

        assertEquals(EstadoSolicitud.APROBADA, response.estado());
        verify(users).save(argThat(value -> value.isActivo() && value.getRol() == Rol.USUARIO_NORMAL));
        verify(devices).save(argThat(value -> value.isActivo() && value.getUsuario() != null));
        verify(links).save(any(VinculacionDispositivo.class));
    }

    @Test void rejectionDoesNotActivateAnything() {
        SolicitudVinculacion request = new SolicitudVinculacion(); request.setId(5L); request.setEstado(EstadoSolicitud.PENDIENTE); request.setNombreUsuarioSolicitado("ana");
        SolicitudVinculacionRepository requests = mock(SolicitudVinculacionRepository.class); when(requests.findById(5L)).thenReturn(Optional.of(request)); when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService service = new AuthService(mock(OrganizacionRepository.class), requests, mock(UsuarioRepository.class), mock(DispositivoRepository.class), mock(VinculacionDispositivoRepository.class), mock(JwtService.class), "server-key", "bootstrap-token");

        LinkingResponseDto response = service.reject(5L, user(1L, Rol.ADMINISTRADOR_SISTEMAS));

        assertEquals(EstadoSolicitud.RECHAZADA, response.estado());
        verify(requests).save(request);
    }

    @Test void bootstrapLinksFirstAdministratorAndReturnsJwt() {
        Organizacion organization = new Organizacion(); organization.setId(1L);
        Usuario admin = user(7L, Rol.ADMINISTRADOR_SISTEMAS); admin.setNombreUsuario("admin"); admin.setOrganizacion(organization);
        OrganizacionRepository organizations = mock(OrganizacionRepository.class); when(organizations.findAll()).thenReturn(java.util.List.of(organization));
        UsuarioRepository users = mock(UsuarioRepository.class); when(users.findByNombreUsuario("admin")).thenReturn(Optional.of(admin)); when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DispositivoRepository devices = mock(DispositivoRepository.class); when(devices.existsByUsuarioIdAndActivoTrue(7L)).thenReturn(false); when(devices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        JwtService jwt = mock(JwtService.class); when(jwt.createToken(7L)).thenReturn("jwt");
        AuthService service = new AuthService(organizations, mock(SolicitudVinculacionRepository.class), users, devices, mock(VinculacionDispositivoRepository.class), jwt, "server-key", "bootstrap-token");

        LoginResponseDto response = service.bootstrapAdmin(new BootstrapAdminRequestDto("bootstrap-token", "Admin User", "Admin phone", "admin-device", "public-key"));

        assertEquals("jwt", response.token());
        assertEquals(Rol.ADMINISTRADOR_SISTEMAS, response.usuario().rol());
        verify(devices).save(argThat(value -> value.isActivo() && value.getUsuario().getId().equals(7L)));
    }

    private Usuario user(Long id, Rol role) { Usuario user = new Usuario(); user.setId(id); user.setRol(role); user.setActivo(true); return user; }
}