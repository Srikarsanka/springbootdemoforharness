package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.dto.request.TaskRequest;
import com.dnb.taskmanager.dto.response.TaskResponse;
import com.dnb.taskmanager.model.enums.TaskStatus;
import com.dnb.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Endpoints for managing tasks and kanban board activities")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse response = taskService.createTask(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all tasks with optional search and filtering")
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) com.dnb.taskmanager.model.enums.TaskPriority priority,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(taskService.getAllTasks(search, status, priority, projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task details by ID")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all tasks for a specific project")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable String projectId,
            @RequestParam(required = false) TaskStatus status) {
        if (status != null) {
            return ResponseEntity.ok(taskService.getTasksByProjectAndStatus(projectId, status));
        }
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get all tasks assigned to the currently logged in user")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getMyTasks(userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update full task details")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.updateTask(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status (used for Kanban drag-and-drop)")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable String id,
            @RequestParam TaskStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.updateStatus(id, status, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task by ID")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

