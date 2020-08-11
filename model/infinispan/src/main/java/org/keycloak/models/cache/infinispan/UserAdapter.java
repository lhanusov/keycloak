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

package org.keycloak.models.cache.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements CachedUserModel {

    private final Supplier<UserModel> modelSupplier;
    protected final CachedUser cached;
    protected final UserCacheSession userProviderCache;
    protected final KeycloakSession keycloakSession;
    protected final RealmModel realm;
    protected volatile UserModel updated;

    public UserAdapter(CachedUser cached, UserCacheSession userProvider, KeycloakSession keycloakSession, RealmModel realm) {
        this.cached = cached;
        this.userProviderCache = userProvider;
        this.keycloakSession = keycloakSession;
        this.realm = realm;
        this.modelSupplier = this::getUserModel;
    }

    @Override
    public String getFirstName() {
        return getFirstAttribute(FIRST_NAME);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(FIRST_NAME, firstName);
    }

    @Override
    public String getLastName() {
        return getFirstAttribute(LAST_NAME);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(LAST_NAME, lastName);
    }

    @Override
    public String getEmail() {
        return getFirstAttribute(EMAIL);
    }

    @Override
    public void setEmail(String email) {
        email = email == null ? null : email.toLowerCase();
        setSingleAttribute(EMAIL, email);
    }

    @Override
    public UserModel getDelegateForUpdate() {
        if (updated == null) {
            userProviderCache.registerUserInvalidation(realm, cached);
            updated = modelSupplier.get();
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
        return updated;
    }

    @Override
    public boolean isMarkedForEviction() {
        return updated != null;
    }

    @Override
    public void invalidate() {
        try {
            getDelegateForUpdate();
        } catch (IllegalStateException ex) {    // Not found in database, thus "invalidated" in the underlying model by definition
            // ignore
        }
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    @Override
    public ConcurrentHashMap getCachedWith() {
        return cached.getCachedWith();
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getUsername() {
        return getFirstAttribute(UserModel.USERNAME);
    }

    @Override
    public void setUsername(String username) {
        username = username==null ? null : username.toLowerCase();
        setSingleAttribute(UserModel.USERNAME, username);
    }

    @Override
    public Long getCreatedTimestamp() {
        // get from cached always as it is immutable
        return cached.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // nothing to do as this value is immutable
    }

    @Override
    public boolean isEnabled() {
        if (updated != null) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (updated != null) return updated.getFirstAttribute(name);
        return cached.getAttributes(modelSupplier).getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (updated != null) return updated.getAttribute(name);
        List<String> result = cached.getAttributes(modelSupplier).get(name);
        return (result == null) ? Collections.emptyList() : result;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (updated != null) return updated.getAttributes();
        return cached.getAttributes(modelSupplier);
    }

    @Override
    public Set<String> getRequiredActions() {
        if (updated != null) return updated.getRequiredActions();
        return cached.getRequiredActions(modelSupplier);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    @Override
    public void addRequiredAction(String action) {
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    @Override
    public boolean isEmailVerified() {
        if (updated != null) return updated.isEmailVerified();
        return cached.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        getDelegateForUpdate();
        updated.setEmailVerified(verified);
    }

    @Override
    public String getFederationLink() {
        if (updated != null) return updated.getFederationLink();
        return cached.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        getDelegateForUpdate();
        updated.setFederationLink(link);
    }

    @Override
    public String getServiceAccountClientLink() {
        if (updated != null) return updated.getServiceAccountClientLink();
        return cached.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        getDelegateForUpdate();
        updated.setServiceAccountClientLink(clientInternalId);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        if (updated != null) return updated.getRealmRoleMappings();
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> realmMappings = new HashSet<>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(realm.getId())) {
                    realmMappings.add(role);
                }
            }
        }
        return realmMappings;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        if (updated != null) return updated.getClientRoleMappings(app);
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> appMappings = new HashSet<>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                if (((ClientModel) container).getId().equals(app.getId())) {
                    appMappings.add(role);
                }
            }
        }
        return appMappings;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (updated != null) return updated.hasRole(role);
        if (cached.getRoleMappings(modelSupplier).contains(role.getId())) return true;

        Set<RoleModel> mappings = getRoleMappings();
        for (RoleModel mapping: mappings) {
           if (mapping.hasRole(role)) return true;
        }
        return RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (updated != null) return updated.getRoleMappings();
        Set<RoleModel> roles = new HashSet<>();
        for (String id : cached.getRoleMappings(modelSupplier)) {
            RoleModel roleById = keycloakSession.roles().getRoleById(realm, id);
            if (roleById == null) {
                // chance that role was removed, so just delete to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappings();
            }
            roles.add(roleById);

        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteRoleMapping(role);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        if (updated != null) return updated.getGroupsStream();
        Set<GroupModel> groups = new LinkedHashSet<>();
        for (String id : cached.getGroups(modelSupplier)) {
            GroupModel groupModel = keycloakSession.groups().getGroupById(realm, id);
            if (groupModel == null) {
                // chance that role was removed, so just delete to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getGroupsStream();
            }
            groups.add(groupModel);

        }
        return groups.stream();
    }

    @Override
    public void joinGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.joinGroup(group);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.leaveGroup(group);
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        if (updated != null) return updated.isMemberOf(group);
        if (cached.getGroups(modelSupplier).contains(group.getId())) return true;
        return RoleUtils.isMember(getGroupsStream(), group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private UserModel getUserModel() {
        return userProviderCache.getDelegate().getUserById(cached.getId(), realm);
    }
}
