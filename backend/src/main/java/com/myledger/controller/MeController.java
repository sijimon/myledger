package com.myledger.controller;

import com.myledger.dto.MeResponse;
import com.myledger.entity.Role;
import com.myledger.security.AppPrincipal;
import com.myledger.security.Tabs;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Returns the caller identity as resolved from the JWT, including the tabs the frontend
 * should show. Owners get every tab; contractors get their granted set.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal AppPrincipal principal) {
        List<String> tabs = principal.role() == Role.ROLE_OWNER
                ? List.copyOf(Tabs.ALL)
                : List.copyOf(principal.tabs());
        return new MeResponse(principal.userId(), principal.email(), principal.role().name(), tabs);
    }
}
