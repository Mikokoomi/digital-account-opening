package com.digitalbank.accountopening.approval;

import com.digitalbank.accountopening.approval.dto.ApprovalCaseResponse;
import com.digitalbank.accountopening.approval.dto.ApproveApprovalCaseRequest;
import com.digitalbank.accountopening.approval.dto.AssignApprovalCaseRequest;
import com.digitalbank.accountopening.approval.dto.RejectApprovalCaseRequest;
import com.digitalbank.accountopening.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/approval-cases")
public class ApprovalCaseController {

    private final ApprovalCaseService approvalCaseService;

    public ApprovalCaseController(ApprovalCaseService approvalCaseService) {
        this.approvalCaseService = approvalCaseService;
    }

    @GetMapping
    public ApiResponse<List<ApprovalCaseResponse>> getApprovalCases(
            @RequestParam(required = false) ApprovalCaseStatus status,
            @RequestParam(required = false) String assignedTo
    ) {
        return ApiResponse.success(
                "Approval cases retrieved successfully",
                approvalCaseService.getApprovalCases(status, assignedTo)
        );
    }

    @GetMapping("/{caseId}")
    public ApiResponse<ApprovalCaseResponse> getApprovalCase(@PathVariable UUID caseId) {
        return ApiResponse.success(
                "Approval case retrieved successfully",
                approvalCaseService.getApprovalCase(caseId)
        );
    }

    @PatchMapping("/{caseId}/assign")
    public ApiResponse<ApprovalCaseResponse> assign(
            @PathVariable UUID caseId,
            @Valid @RequestBody AssignApprovalCaseRequest request
    ) {
        return ApiResponse.success(
                "Approval case assigned successfully",
                approvalCaseService.assign(caseId, request.staffId())
        );
    }

    @PostMapping("/{caseId}/approve")
    public ApiResponse<ApprovalCaseResponse> approve(
            @PathVariable UUID caseId,
            @Valid @RequestBody ApproveApprovalCaseRequest request
    ) {
        return ApiResponse.success(
                "Approval case approved successfully",
                approvalCaseService.approve(caseId, request.staffId())
        );
    }

    @PostMapping("/{caseId}/reject")
    public ApiResponse<ApprovalCaseResponse> reject(
            @PathVariable UUID caseId,
            @Valid @RequestBody RejectApprovalCaseRequest request
    ) {
        return ApiResponse.success(
                "Approval case rejected successfully",
                approvalCaseService.reject(caseId, request.staffId(), request.reason())
        );
    }
}
