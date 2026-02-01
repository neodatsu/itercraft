package com.itercraft.api.infrastructure.web;

import com.itercraft.api.infrastructure.security.SecurityConfig;
import com.itercraft.api.infrastructure.sse.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SseController.class)
@Import(SecurityConfig.class)
class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private OpaqueTokenIntrospector opaqueTokenIntrospector;

    @Test
    void events_shouldReturn200WithEventStream() throws Exception {
        when(sseService.register()).thenReturn(new SseEmitter(Long.MAX_VALUE));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }
}
