package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sneaky.sneaky.dto.analytics.UserEventRequest;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.analytics.UserEventTrackingService;

class UserEventControllerTest {
    private final CurrentUser currentUser = org.mockito.Mockito.mock(CurrentUser.class);
    private final UserEventTrackingService userEventTrackingService =
            org.mockito.Mockito.mock(UserEventTrackingService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserEventController(currentUser, userEventTrackingService))
                .setValidator(validator)
                .build();
    }

    @Test
    void trackEventPublishesForCurrentUserAndReturnsAccepted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(currentUser.getUserId()).thenReturn(userId);

        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content("""
                                {
                                  "productId": "%s",
                                  "type": "SKIP",
                                  "source": "DISCOVERY_FEED",
                                  "position": 4
                                }
                                """.formatted(productId)))
                .andExpect(status().isAccepted());

        verify(userEventTrackingService).track(org.mockito.Mockito.eq(userId), any(UserEventRequest.class));
    }

    @Test
    void trackEventRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content("{\"source\":\"DISCOVERY_FEED\"}"))
                .andExpect(status().isBadRequest());
    }
}
