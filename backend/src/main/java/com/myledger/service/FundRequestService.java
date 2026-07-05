package com.myledger.service;

import com.myledger.dto.CreateFundRequestRequest;
import com.myledger.dto.FundRequestItemRequest;
import com.myledger.dto.FundRequestItemResponse;
import com.myledger.dto.FundRequestResponse;
import com.myledger.entity.FundRequest;
import com.myledger.entity.FundRequestItem;
import com.myledger.entity.Project;
import com.myledger.entity.Role;
import com.myledger.entity.User;
import com.myledger.repository.FundRequestItemRepository;
import com.myledger.repository.FundRequestRepository;
import com.myledger.repository.ProjectRepository;
import com.myledger.repository.UserRepository;
import com.myledger.security.AppPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.List;

/**
 * Fund requests. Contractors create/edit their own (only while DRAFT); owners review.
 * Ownership is always checked against the JWT-resolved user id, never a request param.
 * The total is recomputed from line items on every change.
 */
@Service
public class FundRequestService {

    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String PAID = "PAID";

    private final FundRequestRepository requests;
    private final FundRequestItemRepository items;
    private final ProjectRepository projects;
    private final UserRepository users;

    public FundRequestService(FundRequestRepository requests,
                              FundRequestItemRepository items,
                              ProjectRepository projects,
                              UserRepository users) {
        this.requests = requests;
        this.items = items;
        this.projects = projects;
        this.users = users;
    }

    // ---- Contractor ----

    @Transactional
    public FundRequestResponse create(Long userId, CreateFundRequestRequest req) {
        Project project = projects.findById(req.projectId())
                .orElseThrow(() -> badRequest("Unknown project"));
        if (!"ACTIVE".equals(project.getStatus())) {
            throw badRequest("Project is not active");
        }
        User requester = users.findById(userId).orElseThrow(() -> notFound("User"));

        FundRequest fr = new FundRequest();
        fr.setProject(project);
        fr.setRequester(requester);
        fr.setTitle(req.title().trim());
        fr.setNote(req.note());
        fr.setStatus(DRAFT);
        fr = requests.save(fr);

        // Optional line items supplied with the create form.
        List<FundRequestItemRequest> lines = req.items() == null ? List.of() : req.items();
        for (FundRequestItemRequest line : lines) {
            FundRequestItem item = new FundRequestItem();
            item.setRequestId(fr.getId());
            item.setDescription(line.description().trim());
            item.setQty(line.qty());
            item.setUnitPrice(line.unitPrice());
            items.save(item);
        }
        if (!lines.isEmpty()) {
            recomputeTotal(fr);
        }

        if (req.submit()) {
            if (lines.isEmpty()) {
                throw badRequest("Add at least one line item before submitting");
            }
            fr.setStatus(SUBMITTED);
            requests.save(fr);
        }

        List<FundRequestItemResponse> itemDtos = items.findByRequestIdOrderByIdAsc(fr.getId())
                .stream().map(FundRequestItemResponse::from).toList();
        return FundRequestResponse.detail(fr, itemDtos);
    }

    @Transactional(readOnly = true)
    public List<FundRequestResponse> listMine(Long userId) {
        return requests.findMine(userId).stream().map(FundRequestResponse::summary).toList();
    }

    @Transactional
    public FundRequestItemResponse addItem(Long requestId, Long userId, FundRequestItemRequest req) {
        FundRequest fr = requireOwnedDraft(requestId, userId);
        FundRequestItem item = new FundRequestItem();
        item.setRequestId(fr.getId());
        item.setDescription(req.description().trim());
        item.setQty(req.qty());
        item.setUnitPrice(req.unitPrice());
        FundRequestItem saved = items.save(item);
        recomputeTotal(fr);
        return FundRequestItemResponse.from(saved);
    }

    @Transactional
    public FundRequestItemResponse updateItem(Long requestId, Long itemId, Long userId, FundRequestItemRequest req) {
        FundRequest fr = requireOwnedDraft(requestId, userId);
        FundRequestItem item = requireItem(itemId, fr.getId());
        item.setDescription(req.description().trim());
        item.setQty(req.qty());
        item.setUnitPrice(req.unitPrice());
        FundRequestItem saved = items.save(item);
        recomputeTotal(fr);
        return FundRequestItemResponse.from(saved);
    }

    @Transactional
    public void deleteItem(Long requestId, Long itemId, Long userId) {
        FundRequest fr = requireOwnedDraft(requestId, userId);
        FundRequestItem item = requireItem(itemId, fr.getId());
        items.delete(item);
        recomputeTotal(fr);
    }

    @Transactional
    public FundRequestResponse submit(Long requestId, Long userId) {
        FundRequest fr = requireOwned(requestId, userId);
        if (!DRAFT.equals(fr.getStatus())) {
            throw conflict("Only a draft can be submitted");
        }
        if (items.findByRequestIdOrderByIdAsc(requestId).isEmpty()) {
            throw badRequest("Add at least one line item before submitting");
        }
        fr.setStatus(SUBMITTED);
        return FundRequestResponse.summary(requests.save(fr));
    }

    @Transactional
    public void delete(Long requestId, Long userId) {
        FundRequest fr = requireOwned(requestId, userId);
        if (!DRAFT.equals(fr.getStatus())) {
            throw conflict("Only a draft can be deleted");
        }
        requests.delete(fr); // items cascade
    }

    // ---- Owner ----

    @Transactional(readOnly = true)
    public List<FundRequestResponse> listAll() {
        return requests.findAllDetailed().stream().map(FundRequestResponse::summary).toList();
    }

    @Transactional
    public FundRequestResponse approve(Long id) {
        return transition(id, SUBMITTED, APPROVED, "Only a submitted request can be approved");
    }

    @Transactional
    public FundRequestResponse reject(Long id) {
        return transition(id, SUBMITTED, REJECTED, "Only a submitted request can be rejected");
    }

    @Transactional
    public FundRequestResponse markPaid(Long id) {
        return transition(id, APPROVED, PAID, "Only an approved request can be marked paid");
    }

    // ---- Shared ----

    @Transactional(readOnly = true)
    public FundRequestResponse get(Long id, AppPrincipal principal) {
        FundRequest fr = requests.findByIdDetailed(id).orElseThrow(() -> notFound("Fund request"));
        if (principal.role() != Role.ROLE_OWNER && !fr.getRequester().getId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your fund request");
        }
        List<FundRequestItemResponse> itemDtos = items.findByRequestIdOrderByIdAsc(id)
                .stream().map(FundRequestItemResponse::from).toList();
        return FundRequestResponse.detail(fr, itemDtos);
    }

    // ---- helpers ----

    private FundRequestResponse transition(Long id, String from, String to, String message) {
        FundRequest fr = requests.findByIdDetailed(id).orElseThrow(() -> notFound("Fund request"));
        if (!from.equals(fr.getStatus())) {
            throw conflict(message);
        }
        fr.setStatus(to);
        return FundRequestResponse.summary(requests.save(fr));
    }

    private FundRequest requireOwned(Long requestId, Long userId) {
        FundRequest fr = requests.findById(requestId).orElseThrow(() -> notFound("Fund request"));
        if (!fr.getRequester().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your fund request");
        }
        return fr;
    }

    private FundRequest requireOwnedDraft(Long requestId, Long userId) {
        FundRequest fr = requireOwned(requestId, userId);
        if (!DRAFT.equals(fr.getStatus())) {
            throw conflict("This request is locked (only drafts can be edited)");
        }
        return fr;
    }

    private FundRequestItem requireItem(Long itemId, Long requestId) {
        FundRequestItem item = items.findById(itemId).orElseThrow(() -> notFound("Line item"));
        if (!item.getRequestId().equals(requestId)) {
            throw notFound("Line item");
        }
        return item;
    }

    private void recomputeTotal(FundRequest fr) {
        // Match the NUMERIC(19,4) column scale so the stored and returned totals agree.
        fr.setTotal(items.sumAmount(fr.getId()).setScale(4, RoundingMode.HALF_UP));
        requests.save(fr);
    }

    private static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
