package org.keycloak.testsuite.forms2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testsuite.KeycloakTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class LoginTest extends KeycloakTest {
    @Test
    public void loginSuccess() {
        String keycloakUrl = keycloak.getBaseUrl() + "/admin";

        driver.get(keycloakUrl);

        WebElement username = driver.findElement(By.id("username"));
        username.sendKeys("admin");

        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys("admin");

        WebElement submitButton = driver.findElement(By.name("login"));
        submitButton.click();
        Assertions.assertTrue(driver.getCurrentUrl().contains("admin/master/console/"));
    }
}
