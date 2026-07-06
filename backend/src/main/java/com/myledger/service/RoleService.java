package com.myledger.service;

import com.myledger.dto.RoleRequest;
import com.myledger.dto.RoleResponse;
import com.myledger.entity.RoleTemplate;
import com.myledger.entity.User;
import com.myledger.repository.RoleTemplateRepository;
import com.myledger.repository.UserRepository;
import com.myledger.security.Tabs;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;

/** Manages custom roles (named tab presets) and keeps assigned users' tabs in sync. */
@Service
public class RoleService {

    private final RoleTemplateRepository roles;
    private final UserRepository users;
    private final RefreshTokenService refreshTokens;

    public RoleService(RoleTemplateRepository roles, UserRepository users, RefreshTokenService refreshTokens) {
        this.roles = roles;
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roles.findAllByOrderByNameAsc().stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest req) {
        String name = req.name().trim();
        if (roles.existsByNameIgnoreCase(name)) {
            throw conflict("A role with that name already exists");
        }
        RoleTemplate r = new RoleTemplate();
        r.setName(name);
        r.setTabs(normalizeTabs(req.tabs()));
        return RoleResponse.from(roles.save(r));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest req) {
        RoleTemplate r = roles.findById(id).orElseThrow(RoleService::notFound);
        String name = req.name().trim();
        if (roles.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw conflict("A role with that name already exists");
        }
        r.setName(name);
        r.setTabs(normalizeTabs(req.tabs()));
        RoleTemplate saved = roles.save(r);

        // Propagate the new tab set to everyone holding this role and force re-auth.
        for (User u : users.findByRoleId(id)) {
            u.setTabs(saved.getTabs());
            users.save(u);
            refreshTokens.revokeAllForUser(u.getId());
        }
        return RoleResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        RoleTemplate r = roles.findById(id).orElseThrow(RoleService::notFound);
        if (r.isBuiltIn()) {
            throw conflict("Built-in roles cannot be deleted");
        }
        if (users.existsByRoleId(id)) {
            throw conflict("Role is assigned to users — reassign them first");
        }
        roles.delete(r);
    }

    private static String normalizeTabs(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return "";
        }
        var keys = new LinkedHashSet<String>();
        for (String t : requested) {
            if (t != null) keys.add(t.trim().toUpperCase());
        }
        return Tabs.toCsv(keys);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
