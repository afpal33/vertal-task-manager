package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.dto.ChallengeRequestDto;
import com.fabrizioroot.vertal.dto.ChallengeResponseDto;
import com.fabrizioroot.vertal.dto.LinkingRequestDto;
import com.fabrizioroot.vertal.dto.LinkingResponseDto;
import com.fabrizioroot.vertal.model.Dispositivo;
import com.fabrizioroot.vertal.model.Equipo;
import com.fabrizioroot.vertal.model.MembresiaEquipo;
import com.fabrizioroot.vertal.model.Organizacion;
import com.fabrizioroot.vertal.model.Recordatorio;
import com.fabrizioroot.vertal.model.Rol;
import com.fabrizioroot.vertal.model.Tarea;
import com.fabrizioroot.vertal.model.Usuario;
import com.fabrizioroot.vertal.repository.DispositivoRepository;
import com.fabrizioroot.vertal.repository.EquipoRepository;
import com.fabrizioroot.vertal.repository.MembresiaEquipoRepository;
import com.fabrizioroot.vertal.repository.OrganizacionRepository;
import com.fabrizioroot.vertal.repository.RecordatorioRepository;
import com.fabrizioroot.vertal.repository.TareaRepository;
import com.fabrizioroot.vertal.repository.UsuarioRepository;
import com.fabrizioroot.vertal.security.JwtService;
import com.fabrizioroot.vertal.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "vertal.bootstrap-token=performance-bootstrap-token")
@ActiveProfiles("test")
class AllEndpointsResponseTimeIntegrationTests {
    private static final Duration MAX_RESPONSE_TIME = Duration.ofSeconds(2);
    private static final int DATASET_TASKS = 100;
    private static final int WARM_UP_ITERATIONS = 3;
    private static final int MEASURED_ITERATIONS = 10;

    @LocalServerPort int port;
    @Autowired OrganizacionRepository organizations;
    @Autowired UsuarioRepository users;
    @Autowired DispositivoRepository devices;
    @Autowired EquipoRepository teams;
    @Autowired MembresiaEquipoRepository memberships;
    @Autowired TareaRepository tasks;
    @Autowired RecordatorioRepository reminders;
    @Autowired JwtService jwt;
    @Autowired AuthService auth;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(MAX_RESPONSE_TIME)
            .build();
    private final Map<String, Statistics> results = new LinkedHashMap<>();

    @Test
    void everyApiEndpointRespondsWithinDefinedLimit() throws Exception {
        measure(
                "GET /actuator/health",
                () -> request("GET", "/actuator/health", null, null, null));

        Organizacion bootstrapOrganization = organization("Bootstrap performance organization");
        Usuario initialAdmin = user(bootstrapOrganization, "admin", Rol.ADMINISTRADOR_SISTEMAS);

        measure("POST /api/auth/bootstrap/admin", () -> {
            devices.findAll().stream()
                    .filter(device -> device.getUsuario() != null
                            && device.getUsuario().getId().equals(initialAdmin.getId()))
                    .forEach(devices::delete);
            String suffix = suffix();
            return request(
                    "POST",
                    "/api/auth/bootstrap/admin",
                    "{\"bootstrapToken\":\"performance-bootstrap-token\",\"nombreCompleto\":\"Initial Admin\",\"nombreDispositivo\":\"Bootstrap device\",\"identificadorDispositivo\":\"bootstrap-" + suffix + "\",\"clavePublicaDispositivo\":\"test-public-key\"}",
                    null,
                    null);
        });

        Organizacion organization = organization("All endpoints performance organization");
        Usuario admin = user(organization, "performance-admin-" + suffix(), Rol.ADMINISTRADOR_SISTEMAS);
        Dispositivo adminDevice = device(admin, "performance-admin-device-" + suffix(), "test-public-key");
        String adminToken = jwt.createToken(admin.getId(), adminDevice.getIdentificadorDispositivo());

        KeyPair keyPair = rsaKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        Usuario actor = user(organization, "performance-user-" + suffix(), Rol.USUARIO_NORMAL);
        Dispositivo actorDevice = device(actor, "performance-user-device-" + suffix(), publicKey);
        String actorToken = jwt.createToken(actor.getId(), actorDevice.getIdentificadorDispositivo());

        Usuario managedUser = user(organization, "performance-managed-" + suffix(), Rol.USUARIO_NORMAL);
        device(managedUser, "performance-managed-device-" + suffix(), "test-public-key");
        Equipo team = team(organization, actor, "All endpoints performance team");
        membership(actor, team);
        Usuario assignee = user(organization, "performance-assignee-" + suffix(), Rol.USUARIO_NORMAL);
        membership(assignee, team);
        Tarea baseTask = task(team, actor, "All endpoints base task");
        reminder(baseTask, actor);

        List<Tarea> dataset = new ArrayList<>();
        for (int index = 0; index < DATASET_TASKS; index++) {
            dataset.add(unsavedTask(team, actor, "Dataset task " + index));
        }
        tasks.saveAll(dataset);

        measure("POST /api/auth/linking/request", () -> {
            String suffix = suffix();
            return request(
                    "POST",
                    "/api/auth/linking/request",
                    "{\"nombreUsuarioSolicitado\":\"link-" + suffix + "\",\"nombreDispositivo\":\"Linked device\",\"identificadorDispositivo\":\"link-device-" + suffix + "\",\"clavePublicaDispositivo\":\"test-public-key\"}",
                    null,
                    null);
        });
        measure(
                "POST /api/auth/challenge",
                () -> request(
                        "POST",
                        "/api/auth/challenge",
                        "{\"identificadorDispositivo\":\"" + actorDevice.getIdentificadorDispositivo() + "\"}",
                        null,
                        null));
        measure("POST /api/auth/login", () -> {
            ChallengeResponseDto challenge = auth.challenge(
                    new ChallengeRequestDto(actorDevice.getIdentificadorDispositivo()));
            String signature = sign(challenge.challenge(), keyPair);
            return request(
                    "POST",
                    "/api/auth/login",
                    "{\"challengeId\":\"" + challenge.challengeId() + "\",\"identificadorDispositivo\":\"" + actorDevice.getIdentificadorDispositivo() + "\",\"firma\":\"" + signature + "\"}",
                    null,
                    null);
        });
        measure(
                "GET /api/auth/me",
                () -> request("GET", "/api/auth/me", null, actorToken, actorDevice));

        measure(
                "GET /api/admin/linking/requests",
                () -> request("GET", "/api/admin/linking/requests", null, adminToken, adminDevice));
        measure("POST /api/admin/linking/requests/{id}/approve", () -> {
            LinkingResponseDto pending = pendingRequest("approve");
            return request(
                    "POST",
                    "/api/admin/linking/requests/" + pending.id() + "/approve",
                    "{\"nombreCompleto\":\"Approved User\",\"rol\":\"USUARIO_NORMAL\"}",
                    adminToken,
                    adminDevice);
        });
        measure("POST /api/admin/linking/requests/{id}/reject", () -> {
            LinkingResponseDto pending = pendingRequest("reject");
            return request(
                    "POST",
                    "/api/admin/linking/requests/" + pending.id() + "/reject",
                    null,
                    adminToken,
                    adminDevice);
        });
        measure(
                "GET /api/admin/users",
                () -> request("GET", "/api/admin/users", null, adminToken, adminDevice));
        measure(
                "PUT /api/admin/users/{id}/role",
                () -> request(
                        "PUT",
                        "/api/admin/users/" + managedUser.getId() + "/role",
                        "{\"rol\":\"MANAGER\"}",
                        adminToken,
                        adminDevice));
        measure("DELETE /api/admin/users/{id}", () -> {
            Usuario target = user(organization, "delete-user-" + suffix(), Rol.USUARIO_NORMAL);
            device(target, "delete-user-device-" + suffix(), "test-public-key");
            return request("DELETE", "/api/admin/users/" + target.getId(), null, adminToken, adminDevice);
        });
        measure(
                "GET /api/admin/devices",
                () -> request("GET", "/api/admin/devices", null, adminToken, adminDevice));
        measure("DELETE /api/admin/devices/{id}", () -> {
            Dispositivo target = device(
                    managedUser,
                    "revoke-device-" + suffix(),
                    "test-public-key");
            return request("DELETE", "/api/admin/devices/" + target.getId(), null, adminToken, adminDevice);
        });

        measure("POST /api/teams", () -> request(
                "POST",
                "/api/teams",
                "{\"nombre\":\"Measured team " + suffix() + "\"}",
                actorToken,
                actorDevice));
        measure("GET /api/teams", () -> request("GET", "/api/teams", null, actorToken, actorDevice));
        measure(
                "GET /api/teams/{id}",
                () -> request("GET", "/api/teams/" + team.getId(), null, actorToken, actorDevice));
        measure("POST /api/teams/{id}/members", () -> {
            Usuario member = user(organization, "add-member-" + suffix(), Rol.USUARIO_NORMAL);
            return request(
                    "POST",
                    "/api/teams/" + team.getId() + "/members",
                    "{\"nombreUsuario\":\"" + member.getNombreUsuario() + "\"}",
                    actorToken,
                    actorDevice);
        });
        measure("DELETE /api/teams/{id}/members/{username}", () -> {
            Usuario member = user(organization, "remove-member-" + suffix(), Rol.USUARIO_NORMAL);
            membership(member, team);
            return request(
                    "DELETE",
                    "/api/teams/" + team.getId() + "/members/" + member.getNombreUsuario(),
                    null,
                    actorToken,
                    actorDevice);
        });

        measure("POST /api/tasks", () -> request(
                "POST",
                "/api/tasks",
                taskBody("Created task " + suffix(), team),
                actorToken,
                actorDevice));
        measure("GET /api/tasks", () -> request("GET", "/api/tasks", null, actorToken, actorDevice));
        measure(
                "GET /api/tasks/{id}",
                () -> request("GET", "/api/tasks/" + baseTask.getId(), null, actorToken, actorDevice));
        measure(
                "PUT /api/tasks/{id}",
                () -> request(
                        "PUT",
                        "/api/tasks/" + baseTask.getId(),
                        taskBody("Updated task", team),
                        actorToken,
                        actorDevice));
        measure("DELETE /api/tasks/{id}", () -> {
            Tarea target = task(team, actor, "Delete task " + suffix());
            return request("DELETE", "/api/tasks/" + target.getId(), null, actorToken, actorDevice);
        });
        measure("POST /api/tasks/{id}/assignments", () -> {
            Tarea target = task(team, actor, "Assignment task " + suffix());
            return request(
                    "POST",
                    "/api/tasks/" + target.getId() + "/assignments",
                    "{\"nombreUsuario\":\"" + assignee.getNombreUsuario() + "\"}",
                    actorToken,
                    actorDevice);
        });
        measure(
                "PUT /api/tasks/{id}/status",
                () -> request(
                        "PUT",
                        "/api/tasks/" + baseTask.getId() + "/status",
                        "{\"estado\":\"EN_PROGRESO\"}",
                        actorToken,
                        actorDevice));
        measure(
                "PUT /api/tasks/{id}/complete",
                () -> request(
                        "PUT",
                        "/api/tasks/" + baseTask.getId() + "/complete",
                        null,
                        actorToken,
                        actorDevice));
        measure(
                "POST /api/tasks/{id}/reminders",
                () -> request(
                        "POST",
                        "/api/tasks/" + baseTask.getId() + "/reminders",
                        "{\"fechaHora\":\"" + Instant.now().plusSeconds(3600) + "\"}",
                        actorToken,
                        actorDevice));
        measure(
                "GET /api/tasks/{id}/reminders",
                () -> request(
                        "GET",
                        "/api/tasks/" + baseTask.getId() + "/reminders",
                        null,
                        actorToken,
                        actorDevice));
        measure("DELETE /api/reminders/{id}", () -> {
            Recordatorio target = reminder(baseTask, actor);
            return request(
                    "DELETE",
                    "/api/reminders/" + target.getId(),
                    null,
                    actorToken,
                    actorDevice);
        });

        assertEndpointInventoryIsComplete();
        printSummary();
    }

    private void measure(String endpoint, RequestFactory factory) throws Exception {
        for (int iteration = 0; iteration < WARM_UP_ITERATIONS; iteration++) {
            execute(factory.create());
        }
        List<Long> elapsed = new ArrayList<>();
        for (int iteration = 0; iteration < MEASURED_ITERATIONS; iteration++) {
            elapsed.add(execute(factory.create()));
        }
        Collections.sort(elapsed);
        long maximum = elapsed.get(elapsed.size() - 1);
        long percentile95 = elapsed.get((int) Math.ceil(elapsed.size() * 0.95) - 1);
        double average = elapsed.stream().mapToLong(Long::longValue).average().orElseThrow();
        assertTrue(maximum < MAX_RESPONSE_TIME.toMillis(), endpoint + " tardó " + maximum + " ms");
        results.put(endpoint, new Statistics(average, percentile95, maximum));
    }

    private long execute(HttpRequest request) throws Exception {
        long start = System.nanoTime();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertTrue(
                response.statusCode() >= 200 && response.statusCode() < 300,
                request.method() + " " + request.uri() + " devolvió HTTP " + response.statusCode());
        return elapsed;
    }

    private HttpRequest request(
            String method,
            String path,
            String body,
            String token,
            Dispositivo device) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(MAX_RESPONSE_TIME);
        if (token != null && device != null) {
            builder.header("Authorization", "Bearer " + token)
                    .header("X-Device-Id", device.getIdentificadorDispositivo());
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return builder.build();
    }

    private void assertEndpointInventoryIsComplete() {
        Set<String> exposed = mappings.getHandlerMethods().keySet().stream()
                .filter(info -> info.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/")))
                .flatMap(this::endpointKeys)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> measuredApi = results.keySet().stream()
                .filter(endpoint -> endpoint.contains(" /api/"))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(exposed, measuredApi,
                "PNF01 debe medir todas las rutas declaradas por los controladores");
    }

    private java.util.stream.Stream<String> endpointKeys(RequestMappingInfo info) {
        return info.getPatternValues().stream().flatMap(pattern ->
                info.getMethodsCondition().getMethods().stream()
                        .map(method -> method.name() + " " + pattern));
    }

    private void printSummary() {
        results.forEach((endpoint, statistics) -> System.out.printf(
                Locale.ROOT,
                "PNF01 (RNF04) | %s | repeticiones=%d | promedio=%.2f ms | p95=%d ms | max=%d ms | HTTP 2xx | CONFORME%n",
                endpoint,
                MEASURED_ITERATIONS,
                statistics.average(),
                statistics.percentile95(),
                statistics.maximum()));
        System.out.printf(
                Locale.ROOT,
                "PNF01 (RNF04) | RESUMEN | endpoints=%d | datos=%d tareas | calentamiento=%d | repeticiones=%d por endpoint | límite=%d ms | java=%s | os=%s | CONFORME%n",
                results.size(),
                DATASET_TASKS,
                WARM_UP_ITERATIONS,
                MEASURED_ITERATIONS,
                MAX_RESPONSE_TIME.toMillis(),
                System.getProperty("java.version"),
                System.getProperty("os.name"));
    }

    private Organizacion organization(String name) {
        Organizacion organization = new Organizacion();
        organization.setNombre(name + " " + suffix());
        return organizations.save(organization);
    }

    private Usuario user(Organizacion organization, String username, Rol role) {
        Usuario user = new Usuario();
        user.setNombreUsuario(username);
        user.setNombreCompleto("Performance " + username);
        user.setRol(role);
        user.setOrganizacion(organization);
        return users.save(user);
    }

    private Dispositivo device(Usuario user, String identifier, String publicKey) {
        Dispositivo device = new Dispositivo();
        device.setIdentificadorDispositivo(identifier);
        device.setNombreDispositivo(identifier);
        device.setClavePublicaDispositivo(publicKey);
        device.setClavePublicaServidor("test-server-key");
        device.setUsuario(user);
        return devices.save(device);
    }

    private Equipo team(Organizacion organization, Usuario creator, String name) {
        Equipo team = new Equipo();
        team.setNombre(name);
        team.setCreador(creator);
        team.setOrganizacion(organization);
        return teams.save(team);
    }

    private void membership(Usuario user, Equipo team) {
        MembresiaEquipo membership = new MembresiaEquipo();
        membership.setUsuario(user);
        membership.setEquipo(team);
        memberships.save(membership);
    }

    private Tarea task(Equipo team, Usuario creator, String title) {
        return tasks.save(unsavedTask(team, creator, title));
    }

    private Tarea unsavedTask(Equipo team, Usuario creator, String title) {
        Tarea task = new Tarea();
        task.setTitulo(title);
        task.setDescripcion("Performance endpoint verification");
        task.setEquipo(team);
        task.setCreador(creator);
        return task;
    }

    private Recordatorio reminder(Tarea task, Usuario user) {
        Recordatorio reminder = new Recordatorio();
        reminder.setTarea(task);
        reminder.setUsuario(user);
        reminder.setFechaHora(Instant.now().plusSeconds(3600));
        return reminders.save(reminder);
    }

    private LinkingResponseDto pendingRequest(String prefix) {
        String suffix = suffix();
        return auth.request(new LinkingRequestDto(
                prefix + "-user-" + suffix,
                prefix + " device",
                prefix + "-device-" + suffix,
                "test-public-key"));
    }

    private String taskBody(String title, Equipo team) {
        return "{\"titulo\":\"" + title + "\",\"descripcion\":\"Performance endpoint verification\",\"fechaVencimiento\":\"" + Instant.now().plusSeconds(7200) + "\",\"equipoId\":" + team.getId() + "}";
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String sign(String challenge, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String suffix() {
        return UUID.randomUUID().toString();
    }

    private record Statistics(double average, long percentile95, long maximum) {}

    @FunctionalInterface
    private interface RequestFactory {
        HttpRequest create() throws Exception;
    }
}
