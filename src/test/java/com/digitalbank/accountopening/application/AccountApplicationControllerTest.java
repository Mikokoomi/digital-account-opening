package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotEditableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountApplicationController.class)
class AccountApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Test
    void createApplication_shouldReturnBadRequestForEmptyFields() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "",
                                  "productCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateApplication_shouldReturnUpdatedDraftApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.updateApplication(eq(applicationId), any(UpdateApplicationRequest.class)))
                .thenReturn(response(applicationId, "SAVING_ACCOUNT", ApplicationStatus.DRAFT, null));

        mockMvc.perform(patch("/api/applications/{applicationId}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "SAVING_ACCOUNT" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application updated successfully"))
                .andExpect(jsonPath("$.data.productCode").value("SAVING_ACCOUNT"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateApplication_shouldReturnBadRequestForEmptyProductCode() throws Exception {
        mockMvc.perform(patch("/api/applications/{applicationId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void submitApplication_shouldReturnSubmittedApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.submitApplication(applicationId))
                .thenReturn(response(applicationId, "CURRENT_ACCOUNT", ApplicationStatus.SUBMITTED, OffsetDateTime.now()));

        mockMvc.perform(patch("/api/applications/{applicationId}/submit", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application submitted successfully"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
    }

    @Test
    void updateApplication_shouldReturnConflictWhenApplicationIsNotEditable() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.updateApplication(eq(applicationId), any(UpdateApplicationRequest.class)))
                .thenThrow(new ApplicationNotEditableException());

        mockMvc.perform(patch("/api/applications/{applicationId}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "SAVING_ACCOUNT" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_EDITABLE"));
    }

    private ApplicationResponse response(
            UUID applicationId,
            String productCode,
            ApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationResponse(
                applicationId,
                "CUSTOMER-001",
                productCode,
                "Test Product",
                status,
                null,
                null,
                null,
                submittedAt,
                null,
                now,
                now
        );
    }
}
