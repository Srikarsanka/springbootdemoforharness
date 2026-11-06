package com.dnb.taskmanager.dto.response;

import com.dnb.taskmanager.model.enums.UserRole;

import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        Instant createdAt
) {
}
