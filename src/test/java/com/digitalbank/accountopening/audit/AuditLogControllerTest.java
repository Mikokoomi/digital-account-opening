package com.digitalbank.accountopening.audit;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerTest {
    @Test void returnsApplicationAuditLogs() throws Exception {
        AuditLogService service = mock(AuditLogService.class); UUID id = UUID.randomUUID(); when(service.getByApplication(id)).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuditLogController(service)).build();
        mvc.perform(get("/api/applications/{id}/audit-logs", id)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
}
