package org.keycloak.testsuite;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testsuite.server.EmbeddedKeycloakLifecycle;
import org.keycloak.testsuite.server.KeycloakLifecycle;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.time.Duration;

public abstract class KeycloakTest {
    // Selenium 4 WebDriver
    protected static WebDriver driver;

    // Selenium 4 support for WebDriver BiDi protocol
    protected static BrowsingContext context;

    protected static KeycloakLifecycle keycloak;

    protected static Keycloak adminClient;

    protected static String keycloakUrl;

    @BeforeAll
    static void launchBrowser() throws Throwable {
        ChromeOptions options = new ChromeOptions();
        // Waits for web elements to be ready.
        options.setImplicitWaitTimeout(Duration.ofMillis(1000));
        // Waits for webpage setup
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        // Chromium headless browser
        options.addArguments("--headless=new");
        // Turn on BiDi protocol
        //options.setCapability("webSocketUrl", true);

        driver = new ChromeDriver(options);

        keycloak = new EmbeddedKeycloakLifecycle();
        keycloak.start();

        adminClient = Keycloak.getInstance(keycloak.getBaseUrl(), "master", "admin", "admin", "admin-cli");
        keycloakUrl = keycloak.getBaseUrl();
    }

    @AfterAll
    static void closeBrowser() {
        driver.quit();
        adminClient.close();
        keycloak.stop();
    }

    @BeforeEach
    void createContext() throws IOException {
        // Starting new context per each Test method
        //context = new BrowsingContext(driver, driver.getWindowHandle());
    }

    @AfterEach
    void closeContext() throws IOException {
        //context.close();
    }
}
