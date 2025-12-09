package org.keycloak.testframework.ui.webdriver;

import org.keycloak.testframework.config.Config;

class BinaryResolver {

    public static String resolveChromeBinary() {
        String binary = fromConfig();
        if (binary != null) {
            return binary;
        }

        if (System.getenv("CHROMEWEBDRIVER") != null) {
            String executable = isWindows() ? "chromedriver.exe" : "chromedriver";
            return System.getenv("CHROMEWEBDRIVER") + "/" + executable;
        }

        return null;
    }

    public static String resolveFirefoxBinary() {
        String binary = fromConfig();
        if (binary != null) {
            return binary;
        }

        if (System.getenv("GECKOWEBDRIVER") != null) {
            String executable = isWindows() ? "geckodriver.exe" : "geckodriver";
            return System.getenv("GECKOWEBDRIVER") + "/" + executable;
        }

        return null;
    }

    private static String fromConfig() {
        return Config.getValueTypeConfig(ManagedWebDriver.class, "binary", null, String.class);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
