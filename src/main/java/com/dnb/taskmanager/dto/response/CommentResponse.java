package com.dnb.taskmanager.dto.response;

import java.time.Instant;

public record CommentResponse(
        String id,
        String content,
        String taskId,
        String authorId,
        UserResponse author,
        Instant createdAt
) {
}
