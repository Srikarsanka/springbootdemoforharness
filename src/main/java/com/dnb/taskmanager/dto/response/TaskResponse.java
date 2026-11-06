package com.dnb.taskmanager.dto.response;

import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        String id,
        String key,
        String title,
        String description,
        IssueType issueType,
        Integer storyPoints,
        TaskStatus status,
        TaskPriority priority,
        String projectId,
        String projectName,
        String assigneeId,
        UserResponse assignee,
        String reporterId,
        List<String> labels,
        LocalDate dueDate,
        int commentCount,
        Instant createdAt,
        Instant updatedAt
) {
}
