package org.keycloak.testsuite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.server.ContainerKeycloakLifecycle;
import org.keycloak.testsuite.server.EmbeddedKeycloakLifecycle;
import org.keycloak.testsuite.server.KeycloakLifecycle;

import java.io.IOException;
import java.io.InputStream;

public abstract class KeycloakTest {
    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;

    protected static KeycloakLifecycle keycloak;

    protected static Keycloak adminClient;

    @BeforeAll
    static void launchBrowser() throws Throwable {
        playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
        browser = playwright.chromium().launch(launchOptions);

        keycloak = new EmbeddedKeycloakLifecycle();
        keycloak.start();

        adminClient = Keycloak.getInstance(keycloak.getBaseUrl(), "master", "admin", "admin", "admin-cli");
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
        adminClient.close();
        keycloak.stop();
    }

    @BeforeEach
    void createContextAndPage() throws IOException {
        context = browser.newContext();
        page = context.newPage();
        //importTestRealm("/realm/testrealm.json");
    }

    @AfterEach
    void closeContext() throws IOException {
        context.close();
        //adminClient.realms().realm("test").remove();
    }


    private boolean importTestRealm(String realmJsonPath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream fis = getClass().getResourceAsStream(realmJsonPath)) {
            RealmRepresentation realmRepresentation = mapper.readValue(fis, RealmRepresentation.class);
            adminClient.realms().create(realmRepresentation);
            return true;
        }

    }
}
