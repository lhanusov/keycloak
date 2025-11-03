/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.webauthn;

import jakarta.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.SelectAuthenticatorPage;
import org.keycloak.tests.utils.admin.ApiUtil;
import org.keycloak.tests.webauthn.pages.WebAuthnAuthenticatorsListPage;
import org.keycloak.tests.webauthn.realm.WebAuthnRealmConfigBuilder;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

@KeycloakIntegrationTest
public class WebAuthnRegisterAndLoginTest extends AbstractWebAuthnVirtualTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    SelectAuthenticatorPage selectAuthenticatorPage;

    @BeforeEach
    public void initWebAuthnTest() {
        RealmRepresentation realmRepresentation;
        try {
            realmRepresentation = JsonSerialization.readValue(WebAuthnRegisterAndLoginTest.class.getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);
        } catch (IOException exp) {
            throw new RuntimeException("JSON file cannot be loaded: ",exp);
        }

        List<String> acceptableAaguids = new ArrayList<>();
        acceptableAaguids.add("00000000-0000-0000-0000-000000000000");
        acceptableAaguids.add("6d44ba9b-f6ec-2e49-b930-0c8fe920cb73");

        realmRepresentation.setWebAuthnPolicyAcceptableAaguids(acceptableAaguids);
        setUpVirtualAuthenticator(realmRepresentation);
    }

    @AfterEach
    public void cleanupWebAuthnTest() {
        removeVirtualAuthenticator();
    }

    @Test
    public void registerUserSuccess() throws IOException {
        String username = "registerUserSuccess";
        String email = "registerUserSuccess@email";
        String userId = null;

        managedRealm.admin().update(updateRealmWithDefaultWebAuthnSettings(managedRealm.admin()).build());

        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();

        String authenticatorLabel = SecretGenerator.getInstance().randomString(24);
        registerPage.register("firstName", "lastName", email, username, generatePassword());

        // User was registered. Now he needs to register WebAuthn credential
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        appPage.openAccount();

        // confirm that registration is successfully completed
        userId = events.expectRegister(username, email).assertEvent().getUserId();
        // confirm registration event
        EventRepresentation eventRep1 = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                .assertEvent();
        EventRepresentation eventRep2 = events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                .assertEvent();
        String regPubKeyCredentialId1 = eventRep1.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);
        String regPubKeyCredentialId2 = eventRep2.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

        assertThat(regPubKeyCredentialId1, equalTo(regPubKeyCredentialId2));

        // confirm login event
        String sessionId = events.expectLogin()
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .assertEvent().getSessionId();
        // confirm user registered
        assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());
        assertRegisteredCredentials(userId, ALL_ZERO_AAGUID, "none");

        events.clear();

        // logout by user
        logout();

        // confirm logout event
        events.expectLogout(sessionId)
                .removeDetail(Details.REDIRECT_URI)
                .user(userId)
                .client("account")
                .assertEvent();

        // login by user
        oAuthClient.openLoginForm();
        loginPage.fillLogin(username, getPassword(username));

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(authenticatorLabel));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        appPage.openAccount();

        // confirm login event
        sessionId = events.expectLogin()
                .user(userId)
                .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, regPubKeyCredentialId2)
                .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, Boolean.FALSE.toString())
                .assertEvent().getSessionId();

        events.clear();
        // logout by user
        logout();

        // confirm logout event
        events.expectLogout(sessionId)
                .removeDetail(Details.REDIRECT_URI)
                .client("account")
                .user(userId)
                .assertEvent();
    }

    @Test
    public void webAuthnPasswordlessAlternativeWithWebAuthnAndPassword() throws IOException {
        String userId = null;

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(webAuthnTogetherPasswordlessFlow());

        managedRealm.admin().update(realm);

        UserRepresentation user = ApiUtil.findUserByUsername(managedRealm.admin(), "test-user@localhost");
        assertThat(user, notNullValue());
        user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(userResource, notNullValue());
        userResource.update(user);

        user = userResource.toRepresentation();
        assertThat(user, notNullValue());
        assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

        userId = user.getId();

        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly("test-user@localhost");
        loginPage.submit();

            passwordPage.assertCurrent();
            passwordPage.login(getPassword("test-user@localhost"));

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL)
                .assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL)
                .assertEvent();

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));

        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, PASSWORDLESS_LABEL)
                .assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, PASSWORDLESS_LABEL)
                .assertEvent();

        final String sessionID = events.expectLogin()
                .user(userId)
                .assertEvent()
                .getSessionId();

        events.clear();

        logout();

        events.expectLogout(sessionID)
                .removeDetail(Details.REDIRECT_URI)
                .user(userId)
                .client("account")
                .assertEvent();

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLoginWithUsernameOnly(managedUser.getUsername());
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(managedUser.getPassword());

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(managedUser.getUsername());

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();
    }

    @Test
    public void webAuthnPasswordlessShouldFailIfUserIsDeletedInBetween() throws IOException {

        final String WEBAUTHN_LABEL = "webauthn";
        final String PASSWORDLESS_LABEL = "passwordless";

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(webAuthnTogetherPasswordlessFlow());

        managedRealm.admin().update(realm);

        String username = "webauthn-tester@localhost";
        String password = generatePassword();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setFirstName("WebAuthN");
        user.setLastName("Tester");

        String userId = ApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, password, false);

        user = ApiUtil.findUserByUsername(managedRealm.admin(), username);

        assertThat(user, notNullValue());
        user.getRequiredActions().add(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(userResource, notNullValue());
        userResource.update(user);

        user = userResource.toRepresentation();
        assertThat(user, notNullValue());
        assertThat(user.getRequiredActions(), hasItem(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));

        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(username);
        loginPage.submit();

        passwordPage.assertCurrent();
        passwordPage.login(password);

        events.clear();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(WEBAUTHN_LABEL);

        webAuthnRegisterPage.assertCurrent();

        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL)
                .assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, WEBAUTHN_LABEL)
                .assertEvent();

        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(PASSWORDLESS_LABEL);

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));

        logout();

        // Password + WebAuthn Passkey
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLoginWithUsernameOnly(username);

        passwordPage.assertCurrent();
        passwordPage.login(password);

        webAuthnLoginPage.assertCurrent();

        final WebAuthnAuthenticatorsListPage authenticators = webAuthnLoginPage.getAuthenticators();
        assertThat(authenticators.getCount(), is(1));
        assertThat(authenticators.getLabels(), Matchers.contains(WEBAUTHN_LABEL));

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
        logout();

        // Only passwordless login
        oAuthClient.openLoginForm();
        loginPage.fillLoginWithUsernameOnly(username);

        passwordPage.assertCurrent();
        passwordPage.clickTryAnotherWayLink();

        selectAuthenticatorPage.assertCurrent();
        assertThat(selectAuthenticatorPage.getLoginMethodHelpText(SelectAuthenticatorPage.SECURITY_KEY),
                is("Use your Passkey for passwordless sign in."));
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.SECURITY_KEY);

        webAuthnLoginPage.assertCurrent();
        assertThat(webAuthnLoginPage.getAuthenticators().getCount(), is(0));

        // remove testuser before user authenticates via webauthn
        try (Response resp = managedRealm.admin().users().delete(userId)) {
            // ignore
        }

        webAuthnLoginPage.clickAuthenticate();

        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), is("Unknown user authenticated by the Passkey."));
    }

    @Test
    public void webAuthnTwoFactorAndWebAuthnPasswordlessTogether() throws IOException {
        // Change binding to browser-webauthn-passwordless. This is flow, which contains both "webauthn" and "webauthn-passwordless" authenticator
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow("browser-webauthn-passwordless");
        // Login as test-user@localhost with password
        oAuthClient.openLoginForm();
        loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());

        errorPage.assertCurrent();

        // User is not allowed to register passwordless authenticator in this flow
        assertThat(events.poll().getError(), is("invalid_user_credentials"));
        assertThat(errorPage.getError(), is("Cannot login, credential setup required."));
    }

    private void assertUserRegistered(String userId, String username, String email) {
        UserRepresentation user = getUser(userId);
        assertThat(user, notNullValue());
        assertThat(user.getCreatedTimestamp(), notNullValue());

        // test that timestamp is current with 60s tollerance
        assertThat((System.currentTimeMillis() - user.getCreatedTimestamp()) < 60000, is(true));

        // test user info is set from form
        assertThat(user.getUsername(), is(username.toLowerCase()));
        assertThat(user.getEmail(), is(email.toLowerCase()));
        assertThat(user.getFirstName(), is("firstName"));
        assertThat(user.getLastName(), is("lastName"));
    }

    private void assertRegisteredCredentials(String userId, String aaguid, String attestationStatementFormat) {
        List<CredentialRepresentation> credentials = getCredentials(userId);
        credentials.forEach(i -> {
            if (WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(i.getType())) {
                try {
                    WebAuthnCredentialData data = JsonSerialization.readValue(i.getCredentialData(), WebAuthnCredentialData.class);
                    assertThat(data.getAaguid(), is(aaguid));
                    assertThat(data.getAttestationStatementFormat(), is(attestationStatementFormat));
                } catch (IOException e) {
                    Assertions.fail();
                }
            }
        });
    }

    protected UserRepresentation getUser(String userId) {
        return managedRealm.admin().users().get(userId).toRepresentation();
    }

    protected List<CredentialRepresentation> getCredentials(String userId) {
        return managedRealm.admin().users().get(userId).credentials();
    }

    private static WebAuthnRealmConfigBuilder updateRealmWithDefaultWebAuthnSettings(RealmResource resource) {
        return new WebAuthnRealmConfigBuilder(resource.toRepresentation())
                .setWebAuthnPolicySignatureAlgorithms(Collections.singletonList("ES256"))
                .setWebAuthnPolicyAttestationConveyancePreference("none")
                .setWebAuthnPolicyAuthenticatorAttachment("cross-platform")
                .setWebAuthnPolicyRequireResidentKey("No")
                .setWebAuthnPolicyRpId(null)
                .setWebAuthnPolicyUserVerificationRequirement("preferred")
                .setWebAuthnPolicyAcceptableAaguids(Collections.singletonList(ALL_ZERO_AAGUID));
    }

    /**
     * This flow contains:
     * <p>
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** WebAuthnPasswordlessAuthenticator ALTERNATIVE
     * ** sub-subflow ALTERNATIVE
     * **** PasswordForm ALTERNATIVE
     * **** WebAuthnAuthenticator ALTERNATIVE
     *
     * @return flow alias
     */
    private String webAuthnTogetherPasswordlessFlow() {
        final String newFlowAlias = "browser-together-webauthn-flow";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        runOnServer.run(session -> {
            FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .inForms(forms -> forms
                            .clear()
                            .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                            .addSubFlowExecution(REQUIRED, subFlow -> subFlow
                                    .addAuthenticatorExecution(ALTERNATIVE, WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID)
                                    .addSubFlowExecution(ALTERNATIVE, passwordFlow -> passwordFlow
                                            .addAuthenticatorExecution(REQUIRED, PasswordFormFactory.PROVIDER_ID)
                                            .addAuthenticatorExecution(REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID))
                            ))
                    .defineAsBrowserFlow();
        });
        return newFlowAlias;
    }

    private void removeFirstCredentialForUser(String userId, String credentialType) {
        removeFirstCredentialForUser(userId, credentialType, null);
    }

    /**
     * Remove first occurring credential from user with specific credentialType
     *
     * @param userId          userId
     * @param credentialType  type of credential
     * @param assertUserLabel user label of credential
     */
    private void removeFirstCredentialForUser(String userId, String credentialType, String assertUserLabel) {
        if (userId == null || credentialType == null) return;

        final UserResource userResource = managedRealm.admin().users().get(userId);

        final CredentialRepresentation credentialRep = userResource.credentials()
                .stream()
                .filter(Objects::nonNull)
                .filter(credential -> credentialType.equals(credential.getType()))
                .findFirst()
                .orElse(null);

        if (credentialRep != null) {
            if (assertUserLabel != null) {
                assertThat(credentialRep.getUserLabel(), is(assertUserLabel));
            }
            userResource.removeCredential(credentialRep.getId());
        }
    }
}
