package com.dnb.taskmanager.repository;

import com.dnb.taskmanager.model.Project;
import com.dnb.taskmanager.model.enums.ProjectStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findByOwnerId(String ownerId);

    List<Project> findByMemberIdsContaining(String memberId);

    List<Project> findByStatus(ProjectStatus status);
}
