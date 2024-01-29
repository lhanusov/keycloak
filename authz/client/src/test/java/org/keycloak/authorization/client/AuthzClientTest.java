package org.keycloak.authorization.client;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.ByteArrayInputStream;

@ExtendWith(SystemStubsExtension.class)
public class AuthzClientTest {

    @SystemStub
    public final EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    public void testCreateWithEnvVars() {
        envVars.set("KEYCLOAK_REALM", "test");
        envVars.set("KEYCLOAK_AUTH_SERVER", "http://test");

        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {

            AuthzClient.create(new ByteArrayInputStream(("{\n"
                    + "  \"realm\": \"${env.KEYCLOAK_REALM}\",\n"
                    + "  \"auth-server-url\": \"${env.KEYCLOAK_AUTH_SERVER}\",\n"
                    + "  \"ssl-required\": \"external\",\n"
                    + "  \"enable-cors\": true,\n"
                    + "  \"resource\": \"my-server\",\n"
                    + "  \"credentials\": {\n"
                    + "    \"secret\": \"${env.KEYCLOAK_SECRET}\"\n"
                    + "  },\n"
                    + "  \"confidential-port\": 0,\n"
                    + "  \"policy-enforcer\": {\n"
                    + "    \"enforcement-mode\": \"ENFORCING\"\n"
                    + "  }\n"
                    + "}").getBytes()));
        });

        MatcherAssert.assertThat(runtimeException.getMessage(), Matchers.containsString("Could not obtain configuration from server"));
    }
}
