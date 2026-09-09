package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import com.digitalbank.accountopening.integration.tracking.IntegrationRequestResponse;

@RestController
@RequestMapping("/api/applications")
public class AccountProvisioningController {
    private final ApplicationAccountProvisioningService service;
    private final AccountProvisioningStateService stateService;
    public AccountProvisioningController(ApplicationAccountProvisioningService service, AccountProvisioningStateService stateService) {
        this.service=service; this.stateService=stateService;
    }
    @PostMapping("/{applicationId}/create-account")
    ApiResponse<AccountProvisioningResponse> create(@PathVariable UUID applicationId) {
        return ApiResponse.success("Bank account created successfully", service.createAccount(applicationId));
    }
    @PostMapping("/{applicationId}/retry-account-creation")
    ApiResponse<AccountProvisioningResponse> retry(@PathVariable UUID applicationId) {
        return ApiResponse.success("Bank account creation retried successfully", service.retryAccountCreation(applicationId));
    }
    @GetMapping("/{applicationId}/integration-requests")
    ApiResponse<List<IntegrationRequestResponse>> integrations(@PathVariable UUID applicationId) {
        return ApiResponse.success("Integration requests retrieved successfully", stateService.getIntegrationRequests(applicationId));
    }
}
