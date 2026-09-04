package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class AccountProvisioningController {
    private final ApplicationAccountProvisioningService service;
    public AccountProvisioningController(ApplicationAccountProvisioningService service) { this.service=service; }
    @PostMapping("/{applicationId}/create-account")
    ApiResponse<AccountProvisioningResponse> create(@PathVariable UUID applicationId) {
        return ApiResponse.success("Bank account created successfully", service.createAccount(applicationId));
    }
}
