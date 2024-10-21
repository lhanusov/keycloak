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

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.test.util.AdminEventPaths;
import org.keycloak.test.util.AssertAdminEvents;
import org.keycloak.test.util.admin.ApiUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AuthenticationTestHelper {

    static final String REALM_NAME = "test";

    static final String REQUIRED = "REQUIRED";
    static final String CONDITIONAL = "CONDITIONAL";
    static final String DISABLED = "DISABLED";
    static final String ALTERNATIVE = "ALTERNATIVE";

    static AuthenticationExecutionInfoRepresentation findExecutionByProvider(String provider, List<AuthenticationExecutionInfoRepresentation> executions) {
        for (AuthenticationExecutionInfoRepresentation exec : executions) {
            if (provider.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }

    /**
     * Searches for an execution located before the provided execution on the same level of
     * an authentication flow.
     *
     * @param execution execution to find a neighbor for
     * @param executions list of executions to search in
     * @return execution, or null if not found
     */
    static AuthenticationExecutionInfoRepresentation findPreviousExecution(AuthenticationExecutionInfoRepresentation execution, List<AuthenticationExecutionInfoRepresentation> executions) {
        for (AuthenticationExecutionInfoRepresentation exec : executions) {
            if (exec.getLevel() != execution.getLevel()) {
                continue;
            }
            if (exec.getIndex() == execution.getIndex() - 1) {
                return exec;
            }
        }
        return null;
    }

    static AuthenticationFlowRepresentation findFlowByAlias(String alias, List<AuthenticationFlowRepresentation> flows) {
        for (AuthenticationFlowRepresentation flow : flows) {
            if (alias.equals(flow.getAlias())) {
                return flow;
            }
        }
        return null;
    }

    static void compareExecution(AuthenticationExecutionInfoRepresentation expected, AuthenticationExecutionInfoRepresentation actual) {
        Assertions.assertEquals(expected.getRequirement(), actual.getRequirement(), "Execution requirement - " + actual.getProviderId());
        Assertions.assertEquals(expected.getDisplayName(), actual.getDisplayName(), "Execution display name - " + actual.getProviderId());
        Assertions.assertEquals(expected.getConfigurable(), actual.getConfigurable(), "Execution configurable - " + actual.getProviderId());
        Assertions.assertEquals(expected.getProviderId(), actual.getProviderId(), "Execution provider id - " + actual.getProviderId());
        Assertions.assertEquals(expected.getLevel(), actual.getLevel(), "Execution level - " + actual.getProviderId());
        Assertions.assertEquals(expected.getIndex(), actual.getIndex(), "Execution index - " + actual.getProviderId());
        Assertions.assertEquals(expected.getPriority(), actual.getPriority(), "Execution priority - " + actual.getProviderId());
        Assertions.assertEquals(expected.getAuthenticationFlow(), actual.getAuthenticationFlow(), "Execution authentication flow - " + actual.getProviderId());
        Assertions.assertEquals(expected.getRequirementChoices(), actual.getRequirementChoices(), "Execution requirement choices - " + actual.getProviderId());
    }

    static void compareExecution(AuthenticationExecutionExportRepresentation expected, AuthenticationExecutionExportRepresentation actual) {
        Assertions.assertEquals(expected.getFlowAlias(), actual.getFlowAlias(), "Execution flowAlias - " + actual.getFlowAlias());
        Assertions.assertEquals(expected.getAuthenticator(), actual.getAuthenticator(), "Execution authenticator - " + actual.getAuthenticator());
        Assertions.assertEquals(expected.isUserSetupAllowed(), actual.isUserSetupAllowed(), "Execution userSetupAllowed - " + actual.getAuthenticator());
        Assertions.assertEquals(expected.isAuthenticatorFlow(), actual.isAuthenticatorFlow(), "Execution authenticatorFlow - " + actual.getAuthenticator());
        Assertions.assertEquals(expected.getAuthenticatorConfig(), actual.getAuthenticatorConfig(), "Execution authenticatorConfig - " + actual.getAuthenticatorConfig());
        Assertions.assertEquals(expected.getPriority(), actual.getPriority(), "Execution priority - " + actual.getAuthenticator());
        Assertions.assertEquals(expected.getRequirement(), actual.getRequirement(), "Execution requirement - " + actual.getAuthenticator());
    }

    static void compareExecutions(List<AuthenticationExecutionExportRepresentation> expected, List<AuthenticationExecutionExportRepresentation> actual) {
        Assertions.assertNotNull(actual, "Executions should not be null");
        Assertions.assertEquals(expected.size(), actual.size(), "Size");

        for (int i = 0; i < expected.size(); i++) {
            compareExecution(expected.get(i), actual.get(i));
        }
    }

    static void compareFlows(AuthenticationFlowRepresentation expected, AuthenticationFlowRepresentation actual) {
        Assertions.assertEquals(expected.getAlias(), actual.getAlias(), "Flow alias");
        Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Flow description");
        Assertions.assertEquals(expected.getProviderId(), actual.getProviderId(), "Flow providerId");
        Assertions.assertEquals(expected.isTopLevel(), actual.isTopLevel(), "Flow top level");
        Assertions.assertEquals(expected.isBuiltIn(), actual.isBuiltIn(), "Flow built-in");

        List<AuthenticationExecutionExportRepresentation> expectedExecs = expected.getAuthenticationExecutions();
        List<AuthenticationExecutionExportRepresentation> actualExecs = actual.getAuthenticationExecutions();

        if (expectedExecs == null) {
            Assertions.assertTrue(actualExecs == null || actualExecs.size() == 0, "Executions should be null or empty");
        } else {
            compareExecutions(expectedExecs, actualExecs);
        }
    }

    static AuthenticationFlowRepresentation newFlow(String alias, String description,
                                                       String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    static AuthenticationExecutionInfoRepresentation newExecInfo(String displayName, String providerId, Boolean configurable,
                                                          int level, int index, String requirement, Boolean authFlow, String[] choices,
                                                          int priority) {

        AuthenticationExecutionInfoRepresentation execution = new AuthenticationExecutionInfoRepresentation();
        execution.setRequirement(requirement);
        execution.setDisplayName(displayName);
        execution.setConfigurable(configurable);
        execution.setProviderId(providerId);
        execution.setLevel(level);
        execution.setIndex(index);
        execution.setAuthenticationFlow(authFlow);
        execution.setPriority(priority);
        if (choices != null) {
            execution.setRequirementChoices(Arrays.asList(choices));
        }
        return execution;
    }

    static void addExecInfo(List<AuthenticationExecutionInfoRepresentation> target, String displayName, String providerId, Boolean configurable,
                 int level, int index, String requirement, Boolean authFlow, String[] choices, int priority) {

        AuthenticationExecutionInfoRepresentation exec = newExecInfo(displayName, providerId, configurable, level, index, requirement, authFlow, choices, priority);
        target.add(exec);
    }

    AuthenticatorConfigRepresentation newConfig(String alias, String[] keyvalues) {
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(alias);

        if (keyvalues == null) {
            throw new IllegalArgumentException("keyvalues == null");
        }
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException("keyvalues should have even number of elements");
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < keyvalues.length; i += 2) {
            params.put(keyvalues[i], keyvalues[i + 1]);
        }
        config.setConfig(params);
        return config;
    }

    static String createFlow(Keycloak adminClient, AuthenticationFlowRepresentation flowRep) {
        Response response = adminClient.realm(REALM_NAME).flows().createFlow(flowRep);
        org.keycloak.test.util.Assert.assertEquals(201, response.getStatus());
        response.close();
        String flowId = ApiUtil.getCreatedId(response);
        new AssertAdminEvents().assertEvent(adminClient.realm(REALM_NAME).toRepresentation().getId(), OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authFlowsPath()), flowRep, ResourceType.AUTH_FLOW);
        return flowId;
    }
}
