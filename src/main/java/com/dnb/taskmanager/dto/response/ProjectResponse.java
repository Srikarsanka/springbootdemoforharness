package com.dnb.taskmanager.dto.response;

import com.dnb.taskmanager.model.enums.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProjectResponse(
        String id,
        String key,
        String name,
        String description,
        ProjectStatus status,
        String ownerId,
        UserResponse owner,
        List<String> memberIds,
        int memberCount,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {
}
