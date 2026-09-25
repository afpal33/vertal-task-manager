package com.fabrizioroot.vertal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendPrivacyTests {
    private static final Path PROJECT = Path.of("").toAbsolutePath();
    private static final Path PRODUCTION_SOURCE = PROJECT.resolve("src/main");
    private static final Path RUNTIME_DEPENDENCIES = PROJECT.resolve("target/privacy/runtime-dependencies.txt");
    private static final Pattern DEPENDENCY_COORDINATE = Pattern.compile(
            "(?m)^\\s*([a-zA-Z0-9_.-]+):([a-zA-Z0-9_.-]+):\\S+.*$");

    @Test
    void resolvedRuntimeDependencyGraphContainsOnlyReviewedProviders() throws IOException {
        String dependencies = Files.readString(RUNTIME_DEPENDENCIES).toLowerCase(Locale.ROOT);
        List<String> prohibited = List.of(
                "firebase-analytics",
                "firebase-crashlytics",
                "sentry",
                "appcenter",
                "datadog",
                "newrelic",
                "new-relic",
                "amplitude",
                "mixpanel",
                "segment-analytics");

        prohibited.forEach(dependency -> assertFalse(
                dependencies.contains(dependency),
                "Dependencia transitiva prohibida: " + dependency));

        List<String> reviewedGroups = List.of(
                "ch.qos.logback",
                "com.fasterxml",
                "com.sun.istack",
                "com.zaxxer",
                "commons-logging",
                "io.jsonwebtoken",
                "io.micrometer",
                "jakarta.",
                "net.bytebuddy",
                "org.antlr",
                "org.apache.logging.log4j",
                "org.apache.tomcat.embed",
                "org.aspectj",
                "org.checkerframework",
                "org.eclipse.angus",
                "org.flywaydb",
                "org.glassfish.jaxb",
                "org.hdrhistogram",
                "org.hibernate",
                "org.jboss.logging",
                "org.jspecify",
                "org.latencyutils",
                "org.postgresql",
                "org.projectlombok",
                "org.slf4j",
                "org.springframework",
                "org.yaml",
                "tools.jackson");

        Matcher matcher = DEPENDENCY_COORDINATE.matcher(dependencies);
        int inspected = 0;
        while (matcher.find()) {
            inspected++;
            String group = matcher.group(1);
            assertTrue(
                    reviewedGroups.stream().anyMatch(group::startsWith),
                    () -> "Proveedor no revisado en dependencias resueltas: " + group + ":" + matcher.group(2));
        }
        assertTrue(inspected > 0, "No se generó el inventario de dependencias de ejecución");
        evidence("Árbol transitivo resuelto y limitado a proveedores revisados (" + inspected + " artefactos)");
    }

    @Test
    void productionCodeDoesNotCreateOutboundNetworkClients() throws IOException {
        String source = productionSource();
        List<String> outboundApis = List.of(
                "java.net.http",
                "java.net.urlconnection",
                "java.net.socket",
                "org.springframework.web.client",
                "org.springframework.web.reactive.function.client",
                "okhttp3",
                "org.apache.hc.client");

        outboundApis.forEach(api -> assertFalse(source.contains(api), "Cliente de red saliente detectado: " + api));
        evidence("Sin clientes de red saliente en código de producción");
    }

    @Test
    void backendNeverDefinesOrAcceptsPrivateDeviceKeys() throws IOException {
        String source = productionSource();

        assertFalse(source.contains("claveprivada"));
        assertFalse(source.contains("privatekey"));
        assertTrue(source.contains("clavepublicadispositivo"));
        evidence("El backend maneja la clave pública y no la clave privada del dispositivo");
    }

    @Test
    void productionConfigurationUsesOrganizationManagedInfrastructure() throws IOException {
        String properties = Files.readString(
                PRODUCTION_SOURCE.resolve("resources/application.properties"));

        assertTrue(properties.contains("SPRING_DATASOURCE_URL"));
        assertTrue(properties.contains("jdbc:postgresql://localhost:5432/vertal"));
        assertFalse(properties.toLowerCase(Locale.ROOT).contains("telemetry"));
        assertFalse(properties.toLowerCase(Locale.ROOT).contains("analytics"));
        evidence("Persistencia configurable en PostgreSQL autogestionado");
    }

    private String productionSource() throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_SOURCE.resolve("java"))) {
            StringBuilder source = new StringBuilder();
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(file));
            }
            return source.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        }
    }

    private void evidence(String control) {
        System.out.printf("PNF04 | %s | CONFORME%n", control);
    }
}
