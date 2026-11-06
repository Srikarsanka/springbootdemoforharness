package com.dnb.taskmanager.dto.request;

import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        IssueType issueType,

        Integer storyPoints,

        TaskStatus status,

        TaskPriority priority,

        @NotBlank(message = "Project ID is required")
        String projectId,

        String assigneeId,

        List<String> labels,

        LocalDate dueDate
) {
}
