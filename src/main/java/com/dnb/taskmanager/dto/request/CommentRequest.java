package com.dnb.taskmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
        String content,

        @NotBlank(message = "Task ID is required")
        String taskId
) {
}
