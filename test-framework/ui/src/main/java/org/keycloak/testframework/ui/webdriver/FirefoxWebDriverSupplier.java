package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxDriverService;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;

public class FirefoxWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "firefox";
    }

    @Override
    public WebDriver getWebDriver() {
        FirefoxOptions options = new FirefoxOptions();
        setCommonCapabilities(options);
        options.addArguments("-headless");
        options.addPreference("extensions.update.enabled", "false");
        options.addPreference("app.update.enabled", "false");
        options.addPreference("app.update.auto", "false");

//        String binary = BinaryResolver.resolveFirefoxBinary();
//        if (binary != null) {
//            options.setBinary(binary);
//        }

        FirefoxDriverService service =
                new GeckoDriverService.Builder().withLogLevel(FirefoxDriverLogLevel.TRACE).withLogOutput(System.out).build();

        return new FirefoxDriver(service, options);
    }
}
