package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.exception.GlobalExceptionHandler;
import com.dnb.taskmanager.security.CustomUserDetailsService;
import com.dnb.taskmanager.security.JwtAuthenticationEntryPoint;
import com.dnb.taskmanager.security.JwtTokenProvider;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@DisplayName("Health Endpoint Automated Tests")
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/health - Responds successfully with HTTP 200 when MongoDB is UP")
    void customApiHealth_RespondsUp() throws Exception {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));

        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.mongoStatus").value("UP"))
                .andExpect(jsonPath("$.environment").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/health - Responds with DEGRADED when MongoDB ping fails")
    void customApiHealth_RespondsDegradedWhenMongoDown() throws Exception {
        when(mongoTemplate.executeCommand(any(Document.class))).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.mongoStatus", Matchers.containsString("DOWN")));
    }
}
