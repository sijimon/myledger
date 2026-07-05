package com.myledger.controller;

import com.myledger.dto.CreateFundRequestRequest;
import com.myledger.dto.FundRequestItemRequest;
import com.myledger.dto.FundRequestItemResponse;
import com.myledger.dto.FundRequestResponse;
import com.myledger.security.AppPrincipal;
import com.myledger.service.FundRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fund requests. Any authenticated user (owner or contractor) can raise and edit their
 * own requests; ownership is enforced in the service. Reviewing all requests and acting
 * on them (approve/reject/mark-paid) is owner-only. Detail is visible to the owner and
 * to the requesting user only.
 */
@RestController
@RequestMapping("/api/fund-requests")
public class FundRequestController {

    // Raising/editing a request is open to any authenticated user (owner or contractor);
    // ownership is enforced in the service. Reviewing others' requests is owner-only.
    private static final String ANY = "isAuthenticated()";
    private static final String OWNER = "hasAuthority('ROLE_OWNER')";

    private final FundRequestService service;

    public FundRequestController(FundRequestService service) {
        this.service = service;
    }

    // ---- Raise / edit own request (owner or contractor) ----

    @PostMapping
    @PreAuthorize(ANY)
    @ResponseStatus(HttpStatus.CREATED)
    public FundRequestResponse create(@Valid @RequestBody CreateFundRequestRequest request,
                                      @AuthenticationPrincipal AppPrincipal principal) {
        return service.create(principal.userId(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize(ANY)
    public List<FundRequestResponse> mine(@AuthenticationPrincipal AppPrincipal principal) {
        return service.listMine(principal.userId());
    }

    @PostMapping("/{id}/items")
    @PreAuthorize(ANY)
    @ResponseStatus(HttpStatus.CREATED)
    public FundRequestItemResponse addItem(@PathVariable Long id,
                                           @Valid @RequestBody FundRequestItemRequest request,
                                           @AuthenticationPrincipal AppPrincipal principal) {
        return service.addItem(id, principal.userId(), request);
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize(ANY)
    public FundRequestItemResponse updateItem(@PathVariable Long id,
                                              @PathVariable Long itemId,
                                              @Valid @RequestBody FundRequestItemRequest request,
                                              @AuthenticationPrincipal AppPrincipal principal) {
        return service.updateItem(id, itemId, principal.userId(), request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize(ANY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id, @PathVariable Long itemId,
                           @AuthenticationPrincipal AppPrincipal principal) {
        service.deleteItem(id, itemId, principal.userId());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(ANY)
    public FundRequestResponse submit(@PathVariable Long id, @AuthenticationPrincipal AppPrincipal principal) {
        return service.submit(id, principal.userId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ANY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppPrincipal principal) {
        service.delete(id, principal.userId());
    }

    // ---- Owner ----

    @GetMapping
    @PreAuthorize(OWNER)
    public List<FundRequestResponse> all() {
        return service.listAll();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(OWNER)
    public FundRequestResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(OWNER)
    public FundRequestResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize(OWNER)
    public FundRequestResponse markPaid(@PathVariable Long id) {
        return service.markPaid(id);
    }

    // ---- Shared (ownership-checked in the service) ----

    @GetMapping("/{id}")
    public FundRequestResponse detail(@PathVariable Long id, @AuthenticationPrincipal AppPrincipal principal) {
        return service.get(id, principal);
    }
}
