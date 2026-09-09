package com.digitalbank.accountopening.audit;

import com.digitalbank.accountopening.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/applications")
public class AuditLogController {
    private final AuditLogService service;
    public AuditLogController(AuditLogService service) { this.service = service; }
    @GetMapping("/{applicationId}/audit-logs")
    public ApiResponse<List<AuditLogResponse>> get(@PathVariable UUID applicationId) {
        return ApiResponse.success("Audit logs retrieved successfully", service.getByApplication(applicationId));
    }
}
