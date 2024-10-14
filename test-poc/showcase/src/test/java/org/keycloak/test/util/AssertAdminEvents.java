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

package org.keycloak.test.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AssertAdminEvents implements BeforeEachCallback {

    private AdminEvents adminEvents;
    private Keycloak adminClient;

    public AssertAdminEvents() {

    }

    public AssertAdminEvents(AdminEvents adminEvents, Keycloak adminClient) {
        this.adminEvents = adminEvents;
        this.adminClient = adminClient;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
    }

    public AdminEvent poll() {
        AdminEvent event = fetchNextEvent();
        Assert.assertNotNull(event, "Admin event expected");

        return event;
    }

    public void assertEmpty() {
        AdminEvent event = fetchNextEvent();
        Assert.assertNull(event, "Empty admin event queue expected, but there is " + event);
    }

    public void clear() {
        adminEvents.clear();
    }

    private AdminEvent fetchNextEvent() {
        return adminEvents.poll();
    }

    public ExpectedAdminEvent expect() {
        return new ExpectedAdminEvent();
    }

    public AdminEvent assertEvent(String realmId, OperationType operationType, String resourcePath, ResourceType resourceType) {
        return assertEvent(realmId, operationType, resourcePath, null, resourceType);
    }

    public AdminEvent assertEvent(String realmId, OperationType operationType, Matcher<String> resourcePath, ResourceType resourceType) {
        return assertEvent(realmId, operationType, resourcePath, null, resourceType);
    }

    public AdminEvent assertEvent(String realmId, OperationType operationType, String resourcePath, Object representation, ResourceType resourceType) {
        return assertEvent(realmId, operationType, Matchers.equalTo(resourcePath), representation, resourceType);
    }

    public AdminEvent assertEvent(String realmId, OperationType operationType, Matcher<String> resourcePath, Object representation, ResourceType resourceType) {
        return expect().realmId(realmId)
                .operationType(operationType)
                .resourcePath(resourcePath)
                .resourceType(resourceType)
                .representation(representation)
                .assertEvent();
    }

    public class ExpectedAdminEvent {

        private AdminEvent expected = new AdminEvent();
        private Matcher<String> resourcePath;
        private ResourceType resourceType;
        private Object expectedRep;

        public ExpectedAdminEvent realmId(String realmId) {
            expected.setRealmId(realmId);
            return this;
        }

        public ExpectedAdminEvent realm(RealmRepresentation realm) {
            return realmId(realm.getId());
        }

        public ExpectedAdminEvent operationType(OperationType operationType) {
            expected.setOperationType(operationType);
            updateOperationTypeIfError();
            return this;
        }

        public ExpectedAdminEvent resourcePath(String resourcePath) {
            return resourcePath(Matchers.equalTo(resourcePath));
        }

        public ExpectedAdminEvent resourcePath(Matcher<String> resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public ExpectedAdminEvent resourceType(ResourceType resourceType){
            expected.setResourceType(resourceType);
            return this;
        }

        public ExpectedAdminEvent error(String error) {
            expected.setError(error);
            updateOperationTypeIfError();
            return this;
        }

        private void updateOperationTypeIfError() {
            if (expected.getError() != null && expected.getOperationType() != null) {
                expected.setOperationType(OperationType.valueOf(expected.getOperationType().toString() + "_ERROR"));
            }
        }

        public ExpectedAdminEvent authDetails(String realmId, String clientId, String userId) {
            AuthDetails authDetails = new AuthDetails();
            authDetails.setRealmId(realmId);
            authDetails.setClientId(clientId);
            authDetails.setUserId(userId);
            expected.setAuthDetails(authDetails);
            return this;
        }

        public ExpectedAdminEvent representation(Object representation) {
            this.expectedRep = representation;
            return this;
        }

        public AdminEvent assertEvent() {
            return assertEvent(poll());
        }

        public AdminEvent assertEvent(AdminEvent actual) {
            Assert.assertEquals(expected.getRealmId(), actual.getRealmId());
            assertThat(actual.getResourcePath(), resourcePath);
            Assert.assertEquals(expected.getResourceType(), actual.getResourceType());
            Assert.assertEquals(expected.getOperationType(), actual.getOperationType());

            Assert.assertTrue(ObjectUtil.isEqualOrBothNull(expected.getError(), actual.getError()));

            // AuthDetails
            AuthDetails expectedAuth = expected.getAuthDetails();
            if (expectedAuth == null) {
                expectedAuth = defaultAuthDetails();
            }

            AuthDetails actualAuth = actual.getAuthDetails();
            Assert.assertEquals(expectedAuth.getRealmId(), actualAuth.getRealmId());
            if(expectedAuth.getUserId() != null) {
                Assert.assertEquals(expectedAuth.getUserId(), actualAuth.getUserId());
            }
            if (expectedAuth.getClientId() != null) {
                Assert.assertEquals(expectedAuth.getClientId(), actualAuth.getClientId());
            }

            // Representation comparison
            if (expectedRep != null) {
                if (actual.getRepresentation() == null) {
                    Assert.fail("Expected representation " + expectedRep + " but no representation was available on actual event");
                } else {
                    try {

                        if (expectedRep instanceof List) {
                            // List of roles. All must be available in actual representation
                            List<RoleRepresentation> expectedRoles = (List<RoleRepresentation>) expectedRep;
                            List<RoleRepresentation> actualRoles = JsonSerialization.readValue(new ByteArrayInputStream(actual.getRepresentation().getBytes()), new TypeReference<List<RoleRepresentation>>() {
                            });

                            Map<String, String> expectedRolesMap = new HashMap<>();
                            for (RoleRepresentation role : expectedRoles) {
                                expectedRolesMap.put(role.getId(), role.getName());
                            }

                            Map<String, String> actualRolesMap = new HashMap<>();
                            for (RoleRepresentation role : actualRoles) {
                                actualRolesMap.put(role.getId(), role.getName());
                            }
                            Assert.assertEquals(expectedRolesMap, actualRolesMap);

                        } else if (expectedRep instanceof Map) {
                            Object actualRep = JsonSerialization.readValue(actual.getRepresentation(), Map.class);

                            // Comparing of map representations. All of "expected" key-values must be available on "actual" map from the event
                            Map<?, ?> expectedRepMap = (Map) expectedRep;
                            Map<?, ?> actualRepMap = (Map) actualRep;

                            for (Map.Entry entry : expectedRepMap.entrySet()) {
                                Object expectedValue = entry.getValue();
                                if (expectedValue != null) {
                                    Object actualValue = actualRepMap.get(entry.getKey());
                                    Assert.assertEquals(expectedValue, actualValue, "Map item with key '" + entry.getKey() + "' not equal.");
                                }
                            }
                        } else {
                            Object actualRep = JsonSerialization.readValue(actual.getRepresentation(), expectedRep.getClass());

                            // Reflection-based comparing for other types - compare the non-null fields of "expected" representation with the "actual" representation from the event
                            for (Method method : Reflections.getAllDeclaredMethods(expectedRep.getClass())) {
                                if (method.getParameterCount() == 0 && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                                    Object expectedValue = Reflections.invokeMethod(method, expectedRep);
                                    if (expectedValue != null) {
                                        Object actualValue = Reflections.invokeMethod(method, actualRep);
                                        Assert.assertEquals(expectedValue, actualValue, "Property method '" + method.getName() + "' of representation not equal.");
                                    }
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            }

            return actual;
        }

    }

    private AuthDetails defaultAuthDetails() {
        String accessTokenString = adminClient.tokenManager().getAccessTokenString();
        try {
            JWSInput input = new JWSInput(accessTokenString);
            AccessToken token = input.readJsonContent(AccessToken.class);

            AuthDetails authDetails = new AuthDetails();
            String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
            String realmId = adminClient.realm(realmName).toRepresentation().getId();
            authDetails.setRealmId(realmId);
            authDetails.setUserId(token.getSubject());
            return authDetails;
        } catch (JWSInputException jwe) {
            throw new RuntimeException(jwe);
        }
    }

    public static Matcher<String> isExpectedPrefixFollowedByUuid(final String prefix) {
        return new TypeSafeMatcher<String>() {

            @Override
            protected boolean matchesSafely(String item) {
                int expectedLength = prefix.length() + 1 + org.keycloak.models.utils.KeycloakModelUtils.generateId().length();
                return item.startsWith(prefix) && expectedLength == item.length();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("resourcePath in the format like \"" + prefix + "/<UUID>\"");
            }

        };
    }


}
