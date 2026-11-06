package com.dnb.taskmanager.model;

import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    @Indexed
    private String key;

    private String title;

    private String description;

    @Builder.Default
    private IssueType issueType = IssueType.STORY;

    @Builder.Default
    private Integer storyPoints = 3;

    @Builder.Default
    private TaskStatus status = TaskStatus.BACKLOG;

    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Indexed
    private String projectId;

    @Indexed
    private String assigneeId;

    private String reporterId;

    @Builder.Default
    private List<String> labels = new ArrayList<>();

    private LocalDate dueDate;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
