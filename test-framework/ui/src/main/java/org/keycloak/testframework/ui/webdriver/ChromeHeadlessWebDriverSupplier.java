package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeHeadlessWebDriverSupplier extends AbstractWebDriverSupplier {

    @Override
    public String getAlias() {
        return "chrome-headless";
    }

    @Override
    public WebDriver getWebDriver() {
        ChromeOptions options = new ChromeOptions();
        setCommonCapabilities(options);
        options.addArguments(
                "--headless",
                "--disable-gpu",
                "--window-size=1920,1200",
                "--ignore-certificate-errors",
                "--disable-dev-shm-usage",
                "--no-sandbox"
        );

        String binary = BinaryResolver.resolveChromeBinary();
        if (binary != null) {
            options.setBinary(binary);
        }

        options.addArguments("--verbose --log-path=/tmp/chromedriver.log");


        return new ChromeDriver(options);
    }
}
