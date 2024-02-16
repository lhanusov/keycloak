/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.welcomepage;

import org.junit.jupiter.api.*;
import org.keycloak.testsuite.KeycloakTest;
import org.keycloak.testsuite.auth.page.WelcomePage;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

/**
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WelcomePageTest extends KeycloakTest {

    protected static WelcomePage welcomePage;

    @BeforeAll
    protected static void restartContainer() {
        keycloak.stop();
        keycloak.start();
    }

    @BeforeEach
    protected void welcomePageSetup() {
        welcomePage = new WelcomePage(driver);
    }

    /**
     * Attempt to resolve the floating IP address. This is where EAP/WildFly
     * will be accessible. See "-Djboss.bind.address=0.0.0.0".
     * 
     * @return
     * @throws Exception 
     */
    private String getFloatingIpAddress() throws Exception {
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface ni : Collections.list(netInterfaces)) {
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            for (InetAddress a : Collections.list(inetAddresses)) {
                if (!a.isLoopbackAddress() && a.isSiteLocalAddress()) {
                    return a.getHostAddress();
                }
            }
        }
        return null;
    }

    private URL getPublicServerUrl() throws Exception {
        String floatingIp = getFloatingIpAddress();
        if (floatingIp == null) {
            throw new RuntimeException("Could not determine floating IP address.");
        }
        return new URL("http", floatingIp, keycloak.getPort(), "");
    }

    @Test
    @Order(0)
    public void test_0_CheckProductNameOnWelcomePage() {
        this.welcomePage.navigateTo(keycloakUrl);
        Assertions.assertEquals("Welcome to Keycloak", driver.getTitle());
    }

    @Test
    @Order(1)
    public void test_1_LocalAccessNoAdmin() throws Exception {
        this.welcomePage.navigateTo(keycloakUrl);
        Assertions.assertFalse(this.welcomePage.isPasswordSet(), "Welcome page did not ask to create a new admin user.");
    }

    @Test
    @Order(2)
    public void test_2_RemoteAccessNoAdmin() throws Exception {
        this.welcomePage.navigateTo(getPublicServerUrl().toString());
        Assertions.assertFalse(this.welcomePage.isPasswordSet(), "Welcome page did not ask to create a new admin user.");
    }

    @Test
    @Order(3)
    public void test_3_LocalAccessWithAdmin() throws Exception {
        this.welcomePage.navigateTo(keycloakUrl);
        this.welcomePage.setPassword("admin", "admin");
        Assertions.assertTrue(driver.getPageSource().contains("User created"));

        this.welcomePage.navigateTo(keycloakUrl);
        Assertions.assertTrue(this.welcomePage.isPasswordSet(), "Welcome page asked to set admin password.");
    }

    @Test
    @Order(4)
    public void test_4_RemoteAccessWithAdmin() throws Exception {
        this.welcomePage.navigateTo(getPublicServerUrl().toString());
        Assertions.assertTrue(this.welcomePage.isPasswordSet(), "Welcome page asked to set admin password.");
    }

    @Test
    @Order(5)
    public void test_5_AccessCreatedAdminAccount() throws Exception {
        this.welcomePage.navigateTo(keycloakUrl + "/admin/master/console");
        //this.welcomePage.navigateToAdminConsole();
        Assertions.assertEquals("Keycloak Administration UI", driver.getTitle());
    }
}
