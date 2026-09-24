package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.model.Dispositivo;
import com.fabrizioroot.vertal.model.Organizacion;
import com.fabrizioroot.vertal.model.Rol;
import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.repository.DispositivoRepository;
import com.fabrizioroot.vertal.repository.OrganizacionRepository;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import com.fabrizioroot.vertal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccessControlIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired OrganizacionRepository organizations;
    @Autowired UsuarioRepository users;
    @Autowired DispositivoRepository devices;
    @Autowired JwtService jwt;

    @Test
    void validUserAndDeviceCanAccessProtectedEndpoint() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(fixture))
                        .header("X-Device-Id", fixture.device().getIdentificadorDispositivo()))
                .andExpect(status().isOk());
        evidence("Usuario y dispositivo válidos", 200);
    }

    @Test
    void requestWithoutDeviceHeaderIsRejected() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(fixture)))
                .andExpect(status().isForbidden());
        evidence("Solicitud sin X-Device-Id", 403);
    }

    @Test
    void requestFromDifferentDeviceIsRejected() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(fixture))
                        .header("X-Device-Id", "another-device"))
                .andExpect(status().isForbidden());
        evidence("Dispositivo diferente al JWT", 403);
    }

    @Test
    void revokedDeviceIsRejected() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);
        fixture.device().setActivo(false);
        devices.saveAndFlush(fixture.device());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(fixture))
                        .header("X-Device-Id", fixture.device().getIdentificadorDispositivo()))
                .andExpect(status().isForbidden());
        evidence("Dispositivo revocado", 403);
    }

    @Test
    void deviceOwnedByAnotherUserIsRejected() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);
        Usuario other = user(fixture.organization(), "other", Rol.USUARIO_NORMAL);
        Dispositivo otherDevice = device(other, "other-device");
        String forgedBinding = jwt.createToken(
                fixture.user().getId(),
                otherDevice.getIdentificadorDispositivo());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + forgedBinding)
                        .header("X-Device-Id", otherDevice.getIdentificadorDispositivo()))
                .andExpect(status().isForbidden());
        evidence("Dispositivo perteneciente a otro usuario", 403);
    }

    @Test
    void inactiveUserIsRejected() throws Exception {
        Fixture fixture = fixture(Rol.USUARIO_NORMAL);
        fixture.user().setActivo(false);
        users.saveAndFlush(fixture.user());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(fixture))
                        .header("X-Device-Id", fixture.device().getIdentificadorDispositivo()))
                .andExpect(status().isForbidden());
        evidence("Usuario inactivo", 403);
    }

    @Test
    void systemAdministratorCannotPerformOperationalActions() throws Exception {
        Fixture fixture = fixture(Rol.ADMINISTRADOR_SISTEMAS);

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(fixture))
                        .header("X-Device-Id", fixture.device().getIdentificadorDispositivo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Forbidden team\"}"))
                .andExpect(status().isForbidden());
        evidence("Administrador sin permisos operativos", 403);
    }

    private Fixture fixture(Rol role) {
        String suffix = UUID.randomUUID().toString();
        Organizacion organization = new Organizacion();
        organization.setNombre("Access organization " + suffix);
        organization = organizations.save(organization);
        Usuario user = user(organization, "access-" + suffix, role);
        Dispositivo device = device(user, "device-" + suffix);
        return new Fixture(organization, user, device);
    }

    private Usuario user(Organizacion organization, String username, Rol role) {
        Usuario user = new Usuario();
        user.setNombreUsuario(username);
        user.setNombreCompleto(username);
        user.setRol(role);
        user.setOrganizacion(organization);
        return users.save(user);
    }

    private Dispositivo device(Usuario user, String identifier) {
        Dispositivo device = new Dispositivo();
        device.setIdentificadorDispositivo(identifier);
        device.setNombreDispositivo(identifier);
        device.setClavePublicaDispositivo("test-public-key");
        device.setClavePublicaServidor("test-server-key");
        device.setUsuario(user);
        return devices.save(device);
    }

    private String bearer(Fixture fixture) {
        return "Bearer " + jwt.createToken(
                fixture.user().getId(),
                fixture.device().getIdentificadorDispositivo());
    }

    private void evidence(String scenario, int status) {
        System.out.printf(
                "RNF01 | %s | HTTP %d | CONFORME%n",
                scenario,
                status);
    }

    private record Fixture(
            Organizacion organization,
            Usuario user,
            Dispositivo device) {}
}
