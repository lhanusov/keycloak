package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class FirefoxHeadlessWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "firefox-headless";
    }

    @Override
    public WebDriver getWebDriver() {
        FirefoxOptions options = new FirefoxOptions();
        setCommonCapabilities(options);
        options.addArguments("-headless", "--extensions.update.enabled=false");

        String binary = BinaryResolver.resolveFirefoxBinary();
        if (binary != null) {
            options.setBinary(binary);
        }

        return new FirefoxDriver(options);
    }
}
