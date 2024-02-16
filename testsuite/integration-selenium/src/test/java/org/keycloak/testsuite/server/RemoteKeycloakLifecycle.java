package org.keycloak.testsuite.server;

import org.jboss.logging.Logger;

public class RemoteKeycloakLifecycle implements KeycloakLifecycle {

    private static final Logger logger = Logger.getLogger(RemoteKeycloakLifecycle.class);
    @Override
    public void start() {
        logger.infof("Ignored start of Keycloak server as it is externally managed");
    }

    @Override
    public void stop() {
        logger.infof("Ignored stop of Keycloak server as it is externally managed");
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getBaseUrl() {
        return "";
    }
}
