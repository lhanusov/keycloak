package org.keycloak.testsuite.server;

public interface KeycloakLifecycle {

    void start();

    void stop();

    int getPort();

    String getBaseUrl();
}
