package com.myledger.security;

import com.myledger.entity.Role;

import java.util.Set;

/**
 * The authenticated caller, resolved entirely from the verified JWT.
 *
 * <p>{@code userId} here is the trusted identity for every downstream check — e.g.
 * resolving a contractor's own fund requests — and must never be taken from a request
 * parameter. {@code tabs} are the contractor's granted sections; owners see everything.
 */
public record AppPrincipal(Long userId, String email, Role role, Set<String> tabs) {

    /** Owners have access to every section; contractors only to their granted tabs. */
    public boolean canView(String tab) {
        return role == Role.ROLE_OWNER || tabs.contains(tab);
    }
}
