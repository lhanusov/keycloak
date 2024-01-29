package org.keycloak.testsuite.forms2;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testsuite.KeycloakTest;

public class LoginTest extends KeycloakTest {

    @Test
    public void loginSuccess() {
        String keycloakUrl = keycloak.getBaseUrl() + "/admin";
        page.navigate(keycloakUrl);

        Locator usernameInput = page.locator("input[name=\"username\"]");
        usernameInput.fill("admin");

        Locator passwordInput = page.locator("input[name=\"password\"]");
        passwordInput.fill("admin");

        Locator submitButton = page.locator("input[name=\"login\"]");
        submitButton.click();
        Assertions.assertTrue(page.url().contains("admin/master/console/"));
    }

    @Test
    public void sampleTest(){
    }
}
