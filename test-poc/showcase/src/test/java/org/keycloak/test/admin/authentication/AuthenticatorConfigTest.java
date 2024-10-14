/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.test.admin.authentication;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.InjectEvents;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.events.Events;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.RealmConfig;
import org.keycloak.test.util.AdminEventPaths;
import org.keycloak.test.util.Assert;
import org.keycloak.test.util.AssertAdminEvents;
import org.keycloak.test.util.admin.ApiUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AuthenticatorConfigTest {

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectRealm(lifecycle = LifeCycle.METHOD, config = MyTestRealm.class)
    protected ManagedRealm realm;

    @InjectEvents
    protected Events events;

    @RegisterExtension
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(events, adminClient);

    private String executionId;

    @BeforeEach
    public void beforeConfigTest() {
        AuthenticationFlowRepresentation flowRep = AuthenticationTestHelper.newFlow("firstBrokerLogin2", "firstBrokerLogin2", "basic-flow", true, false);
        AuthenticationTestHelper.createFlow(adminClient, flowRep);

        HashMap<String, Object> params = new HashMap<>();
        params.put("provider", IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        realm.admin().flows().addExecution("firstBrokerLogin2", params);
        assertAdminEvents.assertEvent(realm.admin().toRepresentation().getId(), OperationType.CREATE, AdminEventPaths.authAddExecutionPath("firstBrokerLogin2"), params, ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executionReps = realm.admin().flows().getExecutions("firstBrokerLogin2");
        AuthenticationExecutionInfoRepresentation exec = AuthenticationTestHelper.findExecutionByProvider(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, executionReps);
        Assert.assertNotNull(exec);
        executionId = exec.getId();
    }

    @Test
    public void testCreateConfigWithReservedChar() {
        AuthenticatorConfigRepresentation cfg = newConfig("f!oo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        Response resp = realm.admin().flows().newExecutionConfig(executionId, cfg);
        Assert.assertEquals(400, resp.getStatus());
    }

    @Test
    public void testCreateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Attempt to create config for non-existent execution
        Response response = realm.admin().flows().newExecutionConfig("exec-id-doesnt-exists", cfg);
        Assert.assertEquals(404, response.getStatus());
        response.close();

        // Create config success
        String cfgId = createConfig(executionId, cfg);

        // Assert found
        AuthenticatorConfigRepresentation cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Cleanup
        realm.admin().flows().removeAuthenticatorConfig(cfgId);
        assertAdminEvents.assertEvent(realm.admin().toRepresentation().getId(), OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);
    }

    @Test
    public void testUpdateConfigWithBadChar() {
        assertThrows(BadRequestException.class, () -> {
            AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
            String cfgId = createConfig(executionId, cfg);
            AuthenticatorConfigRepresentation cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgId);

            cfgRep.setAlias("Bad@Char");
            realm.admin().flows().updateAuthenticatorConfig(cfgRep.getId(), cfgRep);
        });
    }

    @Test
    public void testUpdateConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgId);

        // Try to update not existent config
        try {
            realm.admin().flows().updateAuthenticatorConfig("not-existent", cfgRep);
            Assert.fail("Config didn't found");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert nothing changed
        cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgId);
        assertConfig(cfgRep, cfgId, "foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");

        // Update success
        cfgRep.setAlias("foo2");
        cfgRep.getConfig().put("configKey2", "configValue2");
        realm.admin().flows().updateAuthenticatorConfig(cfgRep.getId(), cfgRep);
        assertAdminEvents.assertEvent(realm.admin().toRepresentation().getId(), OperationType.UPDATE, AdminEventPaths.authExecutionConfigPath(cfgId), cfgRep, ResourceType.AUTHENTICATOR_CONFIG);

        // Assert updated
        cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgRep.getId());
        assertConfig(cfgRep, cfgId, "foo2",
                IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true",
                "configKey2", "configValue2");
    }


    @Test
    public void testRemoveConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("foo", IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
        String cfgId = createConfig(executionId, cfg);
        AuthenticatorConfigRepresentation cfgRep = realm.admin().flows().getAuthenticatorConfig(cfgId);

        // Assert execution has our config
        AuthenticationExecutionInfoRepresentation execution = AuthenticationTestHelper.findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, realm.admin().flows().getExecutions("firstBrokerLogin2"));
        Assert.assertEquals(cfgRep.getId(), execution.getAuthenticationConfig());


        // Test remove not-existent
        try {
            realm.admin().flows().removeAuthenticatorConfig("not-existent");
            Assert.fail("Config didn't found");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Test remove our config
        realm.admin().flows().removeAuthenticatorConfig(cfgId);
        assertAdminEvents.assertEvent(realm.admin().toRepresentation().getId(), OperationType.DELETE, AdminEventPaths.authExecutionConfigPath(cfgId), ResourceType.AUTHENTICATOR_CONFIG);

        // Assert config not found
        try {
            realm.admin().flows().getAuthenticatorConfig(cfgRep.getId());
            Assert.fail("Not expected to find config");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert execution doesn't have our config
        execution = AuthenticationTestHelper.findExecutionByProvider(
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, realm.admin().flows().getExecutions("firstBrokerLogin2"));
        Assert.assertNull(execution.getAuthenticationConfig());
    }

    @Test
    public void testNullsafetyIterationOverProperties() {
        String providerId = "auth-cookie";
        String providerName = "Cookie";
        AuthenticatorConfigInfoRepresentation description = realm.admin().flows().getAuthenticatorConfigDescription(providerId);

        Assert.assertEquals(providerName, description.getName());
        Assert.assertTrue(description.getProperties().isEmpty());
    }

    private String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        Response resp = realm.admin().flows().newExecutionConfig(executionId, cfg);
        Assert.assertEquals(201, resp.getStatus());
        String cfgId = ApiUtil.getCreatedId(resp);
        Assert.assertNotNull(cfgId);
        assertAdminEvents.assertEvent(realm.admin().toRepresentation().getId(), OperationType.CREATE, AdminEventPaths.authAddExecutionConfigPath(executionId), cfg, ResourceType.AUTH_EXECUTION);
        return cfgId;
    }

    private AuthenticatorConfigRepresentation newConfig(String alias, String cfgKey, String cfgValue) {
        AuthenticatorConfigRepresentation cfg = new AuthenticatorConfigRepresentation();
        cfg.setAlias(alias);
        Map<String, String> cfgMap = new HashMap<>();
        cfgMap.put(cfgKey, cfgValue);
        cfg.setConfig(cfgMap);
        return cfg;
    }

    private void assertConfig(AuthenticatorConfigRepresentation cfgRep, String id, String alias, String... fields) {
        Assert.assertEquals(id, cfgRep.getId());
        Assert.assertEquals(alias, cfgRep.getAlias());
        Assert.assertMap(cfgRep.getConfig(), fields);
    }

    private static class MyTestRealm implements RealmConfig {

        @Override
        public RealmRepresentation getRepresentation() {
            return builder()
                    .name(AuthenticationTestHelper.REALM_NAME)
                    .build();
        }
    }
}
