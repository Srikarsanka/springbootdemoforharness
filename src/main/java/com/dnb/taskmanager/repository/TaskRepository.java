package com.dnb.taskmanager.repository;

import com.dnb.taskmanager.model.Task;
import com.dnb.taskmanager.model.enums.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByProjectId(String projectId);

    List<Task> findByAssigneeId(String assigneeId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByProjectIdAndStatus(String projectId, TaskStatus status);

    long countByProjectId(String projectId);

    long countByAssigneeIdAndStatus(String assigneeId, TaskStatus status);
}
