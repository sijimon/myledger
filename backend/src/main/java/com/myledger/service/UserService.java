package com.myledger.service;

import com.myledger.dto.CreateUserRequest;
import com.myledger.dto.PasswordResetRequest;
import com.myledger.dto.UpdateUserRequest;
import com.myledger.dto.UserResponse;
import com.myledger.entity.Role;
import com.myledger.entity.User;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.FundRequestRepository;
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

/**
 * Owner-only account administration. Guardrails prevent an owner from locking everyone
 * out (no self role/status change; the last enabled owner is protected) and prevent
 * deleting accounts that own historical data.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final ExpenseRepository expenses;
    private final StoredFileRepository files;
    private final FundRequestRepository fundRequests;

    public UserService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokens,
                       ExpenseRepository expenses,
                       StoredFileRepository files,
                       FundRequestRepository fundRequests) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.expenses = expenses;
        this.files = files;
        this.fundRequests = fundRequests;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw conflict("A user with that email already exists");
        }
        User user = new User(email, passwordEncoder.encode(req.password()), parseRole(req.role()));
        user.setTabs(resolveTabs(req.tabs()));
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req, Long callerId) {
        User user = users.findById(id).orElseThrow(UserService::notFound);

        Role newRole = req.role() != null ? parseRole(req.role()) : user.getRole();
        boolean newEnabled = req.enabled() != null ? req.enabled() : user.isEnabled();
        String newTabs = req.tabs() != null ? resolveTabs(req.tabs()) : user.getTabs();

        boolean roleChanging = newRole != user.getRole();
        boolean enabledChanging = newEnabled != user.isEnabled();
        boolean tabsChanging = !newTabs.equals(user.getTabs());
        if (!roleChanging && !enabledChanging && !tabsChanging) {
            return UserResponse.from(user);
        }

        // Role/status changes to your own account are blocked; tab changes are harmless.
        if ((roleChanging || enabledChanging) && user.getId().equals(callerId)) {
            throw conflict("You cannot change your own role or status");
        }

        // Protect the last enabled owner from being demoted or disabled.
        if (user.getRole() == Role.ROLE_OWNER && user.isEnabled()) {
            boolean losingOwner = (roleChanging && newRole != Role.ROLE_OWNER)
                    || (enabledChanging && !newEnabled);
            if (losingOwner && users.countByRoleAndEnabledTrue(Role.ROLE_OWNER) <= 1) {
                throw conflict("At least one enabled owner is required");
            }
        }

        user.setRole(newRole);
        user.setEnabled(newEnabled);
        user.setTabs(newTabs);
        User saved = users.save(user);

        // Force re-authentication so the change takes effect promptly: role/tabs must
        // re-issue the JWT, and a disable must stop refreshes.
        if (roleChanging || tabsChanging || (enabledChanging && !newEnabled)) {
            refreshTokens.revokeAllForUser(id);
        }
        return UserResponse.from(saved);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest req) {
        User user = users.findById(id).orElseThrow(UserService::notFound);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(user);
        // Invalidate existing sessions so the old password can't linger via refresh.
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
        users.delete(user); // refresh tokens cascade via FK
    }

    /** Normalise requested tabs to a valid CSV; contractors always keep at least Fund Requests. */
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
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
