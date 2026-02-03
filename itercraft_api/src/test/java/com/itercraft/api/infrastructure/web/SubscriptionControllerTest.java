package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.subscription.SubscriptionService;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private OpaqueTokenIntrospector opaqueTokenIntrospector;

    private static final String SUB = "user-sub-123";

    @Test
    void subscribe_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/subscriptions/tondeuse")
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB)))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(subscriptionService).subscribe(SUB, "tondeuse");
    }

    @Test
    void unsubscribe_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/tondeuse")
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(subscriptionService).unsubscribe(SUB, "tondeuse");
    }

    @Test
    void addUsage_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/subscriptions/tondeuse/usages")
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB)))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(subscriptionService).addUsage(SUB, "tondeuse");
    }

    @Test
    void removeUsage_shouldReturn204() throws Exception {
        UUID usageId = UUID.randomUUID();
        mockMvc.perform(delete("/api/subscriptions/tondeuse/usages/" + usageId)
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(subscriptionService).removeUsage(SUB, "tondeuse", usageId);
    }

    @Test
    void subscribe_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/subscriptions/tondeuse").with(csrf()))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void getUserSubscriptions_shouldReturnList() throws Exception {
        when(subscriptionService.getUserSubscriptions(SUB))
                .thenReturn(List.of(new UserSubscriptionDto("tondeuse", "Tondeuse", 3)));

        mockMvc.perform(get("/api/subscriptions")
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceCode").value("tondeuse"))
                .andExpect(jsonPath("$[0].usageCount").value(3));
    }

    @Test
    void getAllServices_shouldReturnList() throws Exception {
        when(subscriptionService.getAllServices())
                .thenReturn(List.of(new ServiceDto("tondeuse", "Tondeuse", "desc")));

        mockMvc.perform(get("/api/services")
                        .with(opaqueToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("tondeuse"));
    }

    @Test
    void getUsageHistory_shouldReturnList() throws Exception {
        UUID usageId = UUID.randomUUID();
        when(subscriptionService.getUsageHistory(SUB, "tondeuse"))
                .thenReturn(List.of(new com.itercraft.api.infrastructure.web.dto.UsageDto(
                        usageId, java.time.OffsetDateTime.now())));

        mockMvc.perform(get("/api/subscriptions/tondeuse/usages")
                        .with(opaqueToken().attributes(attrs -> attrs.put("sub", SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(usageId.toString()));
    }

}
