package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.dto.response.HealthResponse;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(mongoTemplate, "test");
    }

    @Test
    @DisplayName("Should return UP status when MongoDB ping succeeds")
    void getHealth_MongoUp() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));

        ResponseEntity<HealthResponse> response = healthController.getHealth();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().mongoStatus()).isEqualTo("UP");
        assertThat(response.getBody().environment()).isEqualTo("test");
    }

    @Test
    @DisplayName("Should return DEGRADED status when MongoDB ping fails")
    void getHealth_MongoDown() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<HealthResponse> response = healthController.getHealth();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DEGRADED");
        assertThat(response.getBody().mongoStatus()).contains("DOWN");
    }
}

