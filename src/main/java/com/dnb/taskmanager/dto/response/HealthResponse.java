package com.dnb.taskmanager.dto.response;

import java.time.Instant;

public record HealthResponse(
        String status,
        String version,
        String environment,
        Instant timestamp,
        String mongoStatus
) {
}
