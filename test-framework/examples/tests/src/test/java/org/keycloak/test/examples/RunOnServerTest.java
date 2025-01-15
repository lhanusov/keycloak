package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

@KeycloakIntegrationTest
public class RunOnServerTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testRunOnServer() {
        String realmName = realm.getName();
        String groupName = "default-group";

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel g = realm.createGroup(groupName);
            realm.removeGroup(g);
        });

        Assertions.assertTrue(realm.admin().groups().groups().isEmpty());
    }

    @Test
    public void testFetchOnServer() {
        String realmName = realm.getName();
        String groupName = "default-group";

        final String id = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel g = realm.createGroup(groupName);
            return g.getId();
        }, String.class);

        Assertions.assertEquals(groupName, realm.admin().groups().group(id).toRepresentation().getName());
    }
}
