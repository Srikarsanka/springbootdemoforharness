package com.dnb.taskmanager.dto.request;

import com.dnb.taskmanager.model.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
        String name,

        @Size(max = 10, message = "Project key must not exceed 10 characters")
        String key,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        ProjectStatus status,

        List<String> memberIds,

        LocalDate startDate,

        LocalDate endDate
) {
}
