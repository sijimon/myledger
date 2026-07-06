package com.myledger.service;

import com.myledger.dto.CreateUserRequest;
import com.myledger.dto.PasswordResetRequest;
import com.myledger.dto.UpdateUserRequest;
import com.myledger.dto.UserResponse;
import com.myledger.entity.Role;
import com.myledger.entity.RoleTemplate;
import com.myledger.entity.User;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.FundRequestRepository;
import com.myledger.repository.RoleTemplateRepository;
import com.myledger.repository.StoredFileRepository;
import com.myledger.repository.UserRepository;
import com.myledger.security.Tabs;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Owner-only account administration. A user is either an Owner (full access) or a
 * contractor-class account holding a custom role (which sets their tabs). Guardrails
 * prevent locking everyone out and deleting accounts that own data.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final RoleTemplateRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final ExpenseRepository expenses;
    private final StoredFileRepository files;
    private final FundRequestRepository fundRequests;

    public UserService(UserRepository users,
                       RoleTemplateRepository roles,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokens,
                       ExpenseRepository expenses,
                       StoredFileRepository files,
                       FundRequestRepository fundRequests) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.expenses = expenses;
        this.files = files;
        this.fundRequests = fundRequests;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        Map<Long, String> roleNames = roles.findAll().stream()
                .collect(Collectors.toMap(RoleTemplate::getId, RoleTemplate::getName));
        return users.findAllByOrderByIdAsc().stream()
                .map(u -> UserResponse.from(u, roleNames.get(u.getRoleId())))
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw conflict("A user with that email already exists");
        }

        Role secRole;
        String tabs;
        Long roleId = null;
        if (req.roleId() != null) {
            RoleTemplate role = requireRole(req.roleId());
            secRole = Role.ROLE_CONTRACTOR;
            tabs = role.getTabs();
            roleId = role.getId();
        } else {
            if (req.role() == null) {
                throw badRequest("A role is required");
            }
            secRole = parseRole(req.role());
            tabs = resolveTabs(req.tabs());
        }

        User user = new User(email, passwordEncoder.encode(req.password()), secRole);
        user.setTabs(tabs);
        user.setRoleId(roleId);
        return response(users.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req, Long callerId) {
        User user = users.findById(id).orElseThrow(UserService::notFound);

        Role newRole;
        Long newRoleId;
        String newTabs;
        if (req.roleId() != null) {
            RoleTemplate role = requireRole(req.roleId());
            newRole = Role.ROLE_CONTRACTOR;
            newRoleId = role.getId();
            newTabs = role.getTabs();
        } else {
            newRole = req.role() != null ? parseRole(req.role()) : user.getRole();
            newRoleId = newRole == Role.ROLE_OWNER ? null : user.getRoleId();
            newTabs = req.tabs() != null ? resolveTabs(req.tabs()) : user.getTabs();
        }
        boolean newEnabled = req.enabled() != null ? req.enabled() : user.isEnabled();

        boolean roleChanging = newRole != user.getRole() || !Objects.equals(newRoleId, user.getRoleId());
        boolean enabledChanging = newEnabled != user.isEnabled();
        boolean tabsChanging = !newTabs.equals(user.getTabs());
        if (!roleChanging && !enabledChanging && !tabsChanging) {
            return response(user);
        }

        if ((roleChanging || enabledChanging) && user.getId().equals(callerId)) {
            throw conflict("You cannot change your own role or status");
        }

        if (user.getRole() == Role.ROLE_OWNER && user.isEnabled()) {
            boolean losingOwner = (newRole != Role.ROLE_OWNER) || (enabledChanging && !newEnabled);
            if (losingOwner && users.countByRoleAndEnabledTrue(Role.ROLE_OWNER) <= 1) {
                throw conflict("At least one enabled owner is required");
            }
        }

        user.setRole(newRole);
        user.setRoleId(newRoleId);
        user.setEnabled(newEnabled);
        user.setTabs(newTabs);
        User saved = users.save(user);

        if (roleChanging || tabsChanging || (enabledChanging && !newEnabled)) {
            refreshTokens.revokeAllForUser(id);
        }
        return response(saved);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest req) {
        User user = users.findById(id).orElseThrow(UserService::notFound);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(user);
        refreshTokens.revokeAllForUser(id);
    }

    @Transactional
    public void delete(Long id, Long callerId) {
        User user = users.findById(id).orElseThrow(UserService::notFound);
        if (user.getId().equals(callerId)) {
            throw conflict("You cannot delete your own account");
        }
        if (user.getRole() == Role.ROLE_OWNER && users.countByRole(Role.ROLE_OWNER) <= 1) {
            throw conflict("Cannot delete the last owner");
        }
        if (expenses.existsByCreatedBy(id) || files.existsByUploadedBy(id) || fundRequests.existsByRequesterId(id)) {
            throw conflict("User has recorded activity — disable the account instead of deleting");
        }
        users.delete(user);
    }

    private UserResponse response(User u) {
        String roleName = u.getRoleId() == null ? null
                : roles.findById(u.getRoleId()).map(RoleTemplate::getName).orElse(null);
        return UserResponse.from(u, roleName);
    }

    private RoleTemplate requireRole(Long roleId) {
        return roles.findById(roleId).orElseThrow(() -> badRequest("Unknown role"));
    }

    private static String resolveTabs(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return Tabs.FUND_REQUESTS;
        }
        var keys = new LinkedHashSet<String>();
        for (String t : requested) {
            if (t != null) keys.add(t.trim().toUpperCase());
        }
        String csv = Tabs.toCsv(keys);
        return csv.isBlank() ? Tabs.FUND_REQUESTS : csv;
    }

    private static Role parseRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
