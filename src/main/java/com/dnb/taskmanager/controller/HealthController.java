package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application and dependency health checks")
public class HealthController {

    private final MongoTemplate mongoTemplate;
    private final String environment;

    public HealthController(
            MongoTemplate mongoTemplate,
            @Value("${spring.profiles.active:default}") String environment) {
        this.mongoTemplate = mongoTemplate;
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Get application health status and database connectivity")
    public ResponseEntity<HealthResponse> getHealth() {
        String mongoStatus = "UP";
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
        } catch (Exception e) {
            mongoStatus = "DOWN: " + e.getMessage();
        }

        String overallStatus = mongoStatus.startsWith("UP") ? "UP" : "DEGRADED";

        HealthResponse response = new HealthResponse(
                overallStatus,
                "1.0.0",
                environment,
                Instant.now(),
                mongoStatus
        );

        return ResponseEntity.ok(response);
    }
}

