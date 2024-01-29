package org.keycloak.testsuite.server;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class ContainerKeycloakLifecycle implements KeycloakLifecycle {

    @Container
    GenericContainer keycloak;
    @Override
    public void start() {
        keycloak = new GenericContainer(DockerImageName.parse("quay.io/keycloak/keycloak:latest"))
                .withExposedPorts(8080)
                .withCommand("start-dev")
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");
        keycloak.start();
    }

    @Override
    public void stop() {
        keycloak.stop();
    }

    @Override
    public int getPort() {
        return keycloak.getFirstMappedPort();
    }

    @Override
    public String getBaseUrl() {
        return "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort();
    }
}
