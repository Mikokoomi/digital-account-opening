package com.digitalbank.accountopening.notification;

import com.digitalbank.accountopening.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/applications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping("/{applicationId}/notifications")
    public ApiResponse<List<NotificationResponse>> get(@PathVariable UUID applicationId) {
        return ApiResponse.success("Notifications retrieved successfully", service.getByApplication(applicationId));
    }
}
