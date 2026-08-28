package com.digitalbank.accountopening.approval;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.approval.dto.ApprovalCaseResponse;
import com.digitalbank.accountopening.common.exception.ApprovalCaseDecisionNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalCaseController.class)
class ApprovalCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApprovalCaseService approvalCaseService;

    @Test
    void assign_shouldReturnAssignedCase() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(approvalCaseService.assign(caseId, "STAFF001"))
                .thenReturn(response(caseId, ApprovalCaseStatus.ASSIGNED));

        mockMvc.perform(patch("/api/approval-cases/{caseId}/assign", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId":"STAFF001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedTo").value("STAFF001"));
    }

    @Test
    void assign_shouldReturn400ForBlankStaffId() throws Exception {
        mockMvc.perform(patch("/api/approval-cases/{caseId}/assign", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void reject_shouldReturn400ForBlankReason() throws Exception {
        mockMvc.perform(post("/api/approval-cases/{caseId}/reject", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId":"STAFF001","reason":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getCase_shouldReturn404WhenCaseDoesNotExist() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(approvalCaseService.getApprovalCase(caseId))
                .thenThrow(new ApprovalCaseNotFoundException(caseId));

        mockMvc.perform(get("/api/approval-cases/{caseId}", caseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPROVAL_CASE_NOT_FOUND"));
    }

    @Test
    void approve_shouldReturn409WhenDecisionIsNotAllowed() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(approvalCaseService.approve(caseId, "STAFF001"))
                .thenThrow(new ApprovalCaseDecisionNotAllowedException(ApprovalCaseStatus.PENDING));

        mockMvc.perform(post("/api/approval-cases/{caseId}/approve", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId":"STAFF001"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPROVAL_CASE_DECISION_NOT_ALLOWED"));
    }

    @Test
    void getCase_shouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/approval-cases/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_UUID"));
    }

    private ApprovalCaseResponse response(UUID caseId, ApprovalCaseStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-28T09:00:00+07:00");
        return new ApprovalCaseResponse(
                caseId,
                UUID.randomUUID(),
                ApplicationStatus.UNDER_REVIEW,
                "CUSTOMER001",
                "CURRENT_ACCOUNT",
                status,
                "STAFF001",
                "Manual review required",
                null,
                now,
                now,
                null,
                now
        );
    }
}
