package com.dnb.taskmanager.model;

import com.dnb.taskmanager.model.enums.ProjectStatus;
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
@Document(collection = "projects")
public class Project {

    @Id
    private String id;

    @Indexed
    private String key;

    private String name;

    private String description;

    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Indexed
    private String ownerId;

    @Builder.Default
    @Indexed
    private List<String> memberIds = new ArrayList<>();

    private LocalDate startDate;

    private LocalDate endDate;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
