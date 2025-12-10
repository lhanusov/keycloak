package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
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
        options.addArguments("-headless");
        options.addPreference("extensions.update.enabled", "false");
        options.addPreference("app.update.enabled", "false");
        options.addPreference("app.update.auto", "false");

        String binary = BinaryResolver.resolveFirefoxBinary();
        if (binary != null) {
            options.setBinary(binary);
        }

        options.setLogLevel(FirefoxDriverLogLevel.TRACE);

        return new FirefoxDriver(options);
    }
}
