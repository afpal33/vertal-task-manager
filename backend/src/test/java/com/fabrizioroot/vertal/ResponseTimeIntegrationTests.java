package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.model.Dispositivo;
import com.fabrizioroot.vertal.model.Equipo;
import com.fabrizioroot.vertal.model.MembresiaEquipo;
import com.fabrizioroot.vertal.model.Organizacion;
import com.fabrizioroot.vertal.model.Rol;
import com.fabrizioroot.vertal.model.Tarea;
import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.repository.DispositivoRepository;
import com.fabrizioroot.vertal.repository.EquipoRepository;
import com.fabrizioroot.vertal.repository.MembresiaEquipoRepository;
import com.fabrizioroot.vertal.repository.OrganizacionRepository;
import com.fabrizioroot.vertal.repository.TareaRepository;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import com.fabrizioroot.vertal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResponseTimeIntegrationTests {
    private static final Duration MAX_RESPONSE_TIME = Duration.ofSeconds(2);

    @Autowired MockMvc mockMvc;
    @Autowired OrganizacionRepository organizations;
    @Autowired UsuarioRepository users;
    @Autowired DispositivoRepository devices;
    @Autowired EquipoRepository teams;
    @Autowired MembresiaEquipoRepository memberships;
    @Autowired TareaRepository tasks;
    @Autowired JwtService jwt;

    @Test
    void principalUserTeamAndTaskOperationsRespondWithinDefinedLimit() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Organizacion organization = new Organizacion();
        organization.setNombre("Performance organization " + suffix);
        organization = organizations.save(organization);

        Usuario admin = new Usuario();
        admin.setNombreUsuario("performance-admin-" + suffix);
        admin.setNombreCompleto("Performance Admin");
        admin.setRol(Rol.ADMINISTRADOR_SISTEMAS);
        admin.setOrganizacion(organization);
        admin = users.save(admin);
        Dispositivo adminDevice = device(admin, "performance-admin-device-" + suffix);

        Usuario user = new Usuario();
        user.setNombreUsuario("performance-user-" + suffix);
        user.setNombreCompleto("Performance User");
        user.setRol(Rol.USUARIO_NORMAL);
        user.setOrganizacion(organization);
        user = users.save(user);
        Dispositivo userDevice = device(user, "performance-user-device-" + suffix);
        Usuario managedUser = new Usuario();
        managedUser.setNombreUsuario("performance-managed-" + suffix);
        managedUser.setNombreCompleto("Performance Managed User");
        managedUser.setRol(Rol.USUARIO_NORMAL);
        managedUser.setOrganizacion(organization);
        managedUser = users.save(managedUser);

        Equipo team = new Equipo();
        team.setNombre("Performance team");
        team.setCreador(user);
        team.setOrganizacion(organization);
        team = teams.save(team);
        MembresiaEquipo membership = new MembresiaEquipo();
        membership.setUsuario(user);
        membership.setEquipo(team);
        memberships.save(membership);
        Tarea task = new Tarea();
        task.setTitulo("Performance task");
        task.setDescripcion("Initial description");
        task.setEquipo(team);
        task.setCreador(user);
        task = tasks.save(task);

        String adminToken = jwt.createToken(admin.getId(), adminDevice.getIdentificadorDispositivo());
        String userToken = jwt.createToken(user.getId(), userDevice.getIdentificadorDispositivo());

        assertFast("Consultar usuarios", get("/api/admin/users"), adminToken, adminDevice);
        assertFast("Consultar equipos", get("/api/teams"), userToken, userDevice);
        assertFast("Consultar detalle de equipo", get("/api/teams/" + team.getId()), userToken, userDevice);
        assertFast("Consultar tareas", get("/api/tasks"), userToken, userDevice);
        assertFast(
                "Crear equipo",
                post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Created within limit\"}"),
                userToken,
                userDevice);
        assertFast(
                "Incorporar miembro",
                post("/api/teams/" + team.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + managedUser.getNombreUsuario() + "\"}"),
                userToken,
                userDevice);
        assertFast(
                "Retirar miembro",
                delete("/api/teams/" + team.getId() + "/members/" + managedUser.getNombreUsuario()),
                userToken,
                userDevice);
        assertFast(
                "Crear tarea",
                post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Created task\",\"descripcion\":\"Created within limit\",\"equipoId\":" + team.getId() + "}"),
                userToken,
                userDevice);
        assertFast(
                "Editar tarea",
                put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Updated task\",\"descripcion\":\"Updated within limit\",\"equipoId\":" + team.getId() + "}"),
                userToken,
                userDevice);
        assertFast("Eliminar tarea", delete("/api/tasks/" + task.getId()), userToken, userDevice);
        assertFast(
                "Asignar rol",
                put("/api/admin/users/" + managedUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"MANAGER\"}"),
                adminToken,
                adminDevice);
        assertFast(
                "Eliminar usuario",
                delete("/api/admin/users/" + managedUser.getId()),
                adminToken,
                adminDevice);
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

    private void assertFast(
            String operation,
            MockHttpServletRequestBuilder request,
            String token,
            Dispositivo device) throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(request
                        .header("Authorization", "Bearer " + token)
                        .header("X-Device-Id", device.getIdentificadorDispositivo()))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(
                elapsed.compareTo(MAX_RESPONSE_TIME) < 0,
                request + " tardó " + elapsed.toMillis() + " ms");
        System.out.printf(
                "RNF04 | %s | HTTP 2xx | %d ms | CONFORME%n",
                operation,
                elapsed.toMillis());
    }
}
