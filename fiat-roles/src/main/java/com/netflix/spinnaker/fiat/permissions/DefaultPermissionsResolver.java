/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.fiat.permissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.netflix.spinnaker.fiat.config.FiatAdminConfig;
import com.netflix.spinnaker.fiat.config.UnrestrictedResourceConfig;
import com.netflix.spinnaker.fiat.model.UserPermission;
import com.netflix.spinnaker.fiat.model.resources.Resource;
import com.netflix.spinnaker.fiat.model.resources.Role;
import com.netflix.spinnaker.fiat.model.resources.ServiceAccount;
import com.netflix.spinnaker.fiat.providers.ProviderException;
import com.netflix.spinnaker.fiat.providers.ResourceProvider;
import com.netflix.spinnaker.fiat.roles.UserRolesProvider;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Slf4j
public class DefaultPermissionsResolver implements PermissionsResolver {

  private final UserRolesProvider userRolesProvider;
  private final ResourceProvider<ServiceAccount> serviceAccountProvider;
  private final ImmutableList<ResourceProvider<? extends Resource>> resourceProviders;
  private final FiatAdminConfig fiatAdminConfig;
  private final ObjectMapper mapper;

  @Autowired
  public DefaultPermissionsResolver(
      UserRolesProvider userRolesProvider,
      ResourceProvider<ServiceAccount> serviceAccountProvider,
      List<ResourceProvider<? extends Resource>> resourceProviders,
      FiatAdminConfig fiatAdminConfig,
      @Qualifier("objectMapper") ObjectMapper mapper) {
    this.userRolesProvider = userRolesProvider;
    this.serviceAccountProvider = serviceAccountProvider;
    this.resourceProviders = ImmutableList.copyOf(resourceProviders);
    this.fiatAdminConfig = fiatAdminConfig;
    this.mapper = mapper;
  }

  @Override
  public UserPermission resolveUnrestrictedUser() {
    return getUserPermission(
        UnrestrictedResourceConfig.UNRESTRICTED_USERNAME,
        new HashSet<>(userRolesProvider.loadUnrestrictedRoles()));
  }

  @Override
  public UserPermission resolve(@NonNull String userId) {
    return resolveAndMerge(new ExternalUser().setId(userId));
  }

  @Override
  public UserPermission resolveAndMerge(@NonNull ExternalUser user) {
    List<Role> roles;
    try {
      log.debug("Loading roles for user " + user);
      roles = userRolesProvider.loadRoles(user);
      log.debug("Got roles " + roles + " for user " + user);
    } catch (ProviderException pe) {
      throw new PermissionResolutionException(
          "Failed to resolve user permission for user " + user.getId(), pe);
    }
    Set<Role> combo =
        Stream.concat(roles.stream(), user.getExternalRoles().stream()).collect(Collectors.toSet());

    return getUserPermission(user.getId(), combo);
  }

  @Override
  public void clearCache() {
    for (ResourceProvider provider : resourceProviders) {
      provider.clearCache();
    }
  }

  private boolean resolveAdminRole(Set<Role> roles) {
    List<String> adminRoles = fiatAdminConfig.getAdmin().getRoles();
    return roles.stream().map(Role::getName).anyMatch(adminRoles::contains);
  }

  @SuppressWarnings("unchecked")
  private UserPermission getUserPermission(String userId, Set<Role> roles) {
    UserPermission permission =
        new UserPermission().setId(userId).setRoles(roles).setAdmin(resolveAdminRole(roles));

    for (ResourceProvider provider : resourceProviders) {
      try {
        if (UnrestrictedResourceConfig.UNRESTRICTED_USERNAME.equalsIgnoreCase(userId)) {
          permission.addResources(provider.getAllUnrestricted());
        }

        if (!roles.isEmpty()) {
          permission.addResources(provider.getAllRestricted(userId, roles, permission.isAdmin()));
        }
      } catch (ProviderException pe) {
        throw new PermissionResolutionException(
            String.format(
                "permission resolution failed from provider %s",
                provider.getClass().getSimpleName()),
            pe);
      }
    }
    return permission;
  }

  @Override
  public Map<String, UserPermission> resolve(@NonNull Collection<ExternalUser> users) {
    StopWatch watch = new StopWatch("resolve.start");
    watch.start();
    Map<String, Collection<Role>> allServiceAccountRoles = getServiceAccountRoles();

    Collection<ExternalUser> serviceAccounts =
        users.stream()
            .filter(user -> allServiceAccountRoles.containsKey(user.getId()))
            .collect(Collectors.toList());

    // Service accounts should already have external roles set. Remove them from the list so they
    // are not sent to the RoleProvider for fetching roles.
    users.removeAll(serviceAccounts);

    Map<String, Collection<Role>> userToRoles = new HashMap<>();

    if (!users.isEmpty()) {
      userToRoles.putAll(getAndMergeUserRoles(users));
    }

    userToRoles.putAll(
        serviceAccounts.stream()
            .collect(Collectors.toMap(ExternalUser::getId, ExternalUser::getExternalRoles)));
    long startTime = System.currentTimeMillis();
    Map<String, UserPermission> resolvedResources = resolveResources(userToRoles);
    long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
    log.info("*** {}s to create {} UserPermission objects", elapsedSeconds, userToRoles.size());
    watch.stop();
    log.info("*** {} or {}s", watch.shortSummary(), watch.getTotalTimeSeconds());
    return resolvedResources;
  }

  private Map<String, Collection<Role>> getServiceAccountRoles() {
    StopWatch watch = new StopWatch("getServiceAccountRoles.method");
    watch.start();
    Map<String, Collection<Role>> serviceAccountRoles =
        serviceAccountProvider.getAll().stream()
            .map(ServiceAccount::toUserPermission)
            .collect(Collectors.toMap(UserPermission::getId, UserPermission::getRoles));
    watch.stop();
    log.info("*** {} or {}s", watch.shortSummary(), watch.getTotalTimeSeconds());
    return serviceAccountRoles;
  }

  private Map<String, Collection<Role>> getAndMergeUserRoles(
      @NonNull Collection<ExternalUser> users) {
    StopWatch watch = new StopWatch("getAndMergeUserRoles.method");
    watch.start();
    Map<String, Collection<Role>> userToRoles = userRolesProvider.multiLoadRoles(users);

    users.forEach(
        user ->
            userToRoles
                .computeIfAbsent(user.getId(), ignored -> new ArrayList<>())
                .addAll(user.getExternalRoles()));

    if (log.isDebugEnabled()) {
      try {
        log.debug(
            "Multi-loaded roles: \n"
                + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userToRoles));
      } catch (Exception e) {
        log.debug("Exception writing roles", e);
      }
    }
    watch.stop();
    log.info("*** {} or {}s", watch.shortSummary(), watch.getTotalTimeSeconds());
    return userToRoles;
  }

  private Map<String, UserPermission> resolveResources(
      @NonNull Map<String, Collection<Role>> userToRoles) {
    StopWatch watch = new StopWatch("DefaultPermissionsResolver.resolveResources");
    watch.start();
    Map<String, UserPermission> resolved =
        userToRoles.entrySet().stream()
            .map(
                entry -> {
                  String userId = entry.getKey();
                  Set<Role> userRoles = new HashSet<>(entry.getValue());

                  return new UserPermission()
                      .setId(userId)
                      .setRoles(userRoles)
                      .setAdmin(resolveAdminRole(userRoles))
                      .addResources(getResources(userId, userRoles, resolveAdminRole(userRoles)));
                })
            .collect(Collectors.toMap(UserPermission::getId, Function.identity()));
    watch.stop();
    log.info("*** {}, or {}s", watch.shortSummary(), watch.getTotalTimeSeconds());
    return resolved;
  }

  private Set<Resource> getResources(String userId, Set<Role> userRoles, boolean isAdmin) {
    return resourceProviders.stream()
        .flatMap(
            provider -> {
              try {
                return provider.getAllRestricted(userId, userRoles, isAdmin).stream();
              } catch (ProviderException pe) {
                throw new PermissionResolutionException(
                    String.format(
                        "resource lookup failed from provider %s",
                        provider.getClass().getSimpleName()),
                    pe);
              }
            })
        .collect(Collectors.toSet());
  }
}
