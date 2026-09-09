package com.digitalbank.accountopening.notification;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest {
    @Test void returnsApplicationNotifications() throws Exception {
        NotificationService service = mock(NotificationService.class); UUID id = UUID.randomUUID(); when(service.getByApplication(id)).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();
        mvc.perform(get("/api/applications/{id}/notifications", id)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
}
