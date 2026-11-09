package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.dto.request.TaskRequest;
import com.dnb.taskmanager.dto.response.TaskResponse;
import com.dnb.taskmanager.exception.GlobalExceptionHandler;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import com.dnb.taskmanager.security.CustomUserDetailsService;
import com.dnb.taskmanager.security.JwtAuthenticationEntryPoint;
import com.dnb.taskmanager.security.JwtTokenProvider;
import com.dnb.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("TaskController REST API Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private TaskResponse sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new TaskResponse(
                "task-123",
                "CORE-101",
                "Automate Build Verification",
                "Write thorough API tests with MockMvc",
                IssueType.STORY,
                5,
                TaskStatus.TODO,
                TaskPriority.HIGH,
                "proj-1",
                "Core Infrastructure",
                "user-1",
                null,
                "user-reporter-1",
                List.of("qa", "automation"),
                LocalDate.now().plusDays(5),
                0,
                Instant.now(),
                Instant.now()
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/tasks
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /api/tasks - Returns HTTP 200 with task list")
    void getAllTasks_Returns200WithList() throws Exception {
        when(taskService.getAllTasks(null, null, null, null)).thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-123"))
                .andExpect(jsonPath("$[0].key").value("CORE-101"))
                .andExpect(jsonPath("$[0].title").value("Automate Build Verification"));
    }

    @Test
    @DisplayName("GET /api/tasks - Returns empty list when no tasks exist")
    void getAllTasks_Empty_Returns200WithEmptyList() throws Exception {
        when(taskService.getAllTasks(null, null, null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/tasks - Supports search and filtering parameters")
    void getAllTasks_WithFilters_ReturnsFilteredList() throws Exception {
        when(taskService.getAllTasks("Build", TaskStatus.TODO, TaskPriority.HIGH, "proj-1"))
                .thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/api/tasks")
                        .param("search", "Build")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("projectId", "proj-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Automate Build Verification"));
    }

    // -------------------------------------------------------------------------
    // GET /api/tasks/{id}
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /api/tasks/{id} - Returns HTTP 200 for existing task")
    void getTaskById_Existing_Returns200() throws Exception {
        when(taskService.getTaskById("task-123")).thenReturn(sampleTask);

        mockMvc.perform(get("/api/tasks/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-123"))
                .andExpect(jsonPath("$.title").value("Automate Build Verification"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Returns HTTP 404 for nonexistent task")
    void getTaskById_NotFound_Returns404() throws Exception {
        when(taskService.getTaskById("invalid-id"))
                .thenThrow(new ResourceNotFoundException("Task not found with id: invalid-id"));

        mockMvc.perform(get("/api/tasks/invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: invalid-id"));
    }

    // -------------------------------------------------------------------------
    // POST /api/tasks
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("POST /api/tasks - Returns HTTP 201 for successful creation")
    void createTask_Valid_Returns201() throws Exception {
        TaskRequest request = new TaskRequest(
                "Automate Build Verification", "Write thorough API tests with MockMvc",
                IssueType.STORY, 5, TaskStatus.TODO, TaskPriority.HIGH,
                "proj-1", "user-1", List.of("qa"), LocalDate.now().plusDays(5));

        when(taskService.createTask(any(TaskRequest.class), anyString())).thenReturn(sampleTask);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("task-123"))
                .andExpect(jsonPath("$.title").value("Automate Build Verification"));
    }

    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("POST /api/tasks - Validates mandatory fields and returns HTTP 400")
    void createTask_InvalidMandatoryFields_Returns400() throws Exception {
        String invalidPayload = "{\"title\":\"\",\"projectId\":\"\"}";

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/tasks/{id}
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("PUT /api/tasks/{id} - Successfully updates existing task and returns HTTP 200")
    void updateTask_Existing_Returns200() throws Exception {
        TaskRequest updateRequest = new TaskRequest(
                "Updated Title", "Updated Description",
                IssueType.TASK, 3, TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM,
                "proj-1", "user-1", List.of("updated"), LocalDate.now().plusDays(10));

        TaskResponse updatedResponse = new TaskResponse(
                "task-123", "CORE-101", "Updated Title", "Updated Description",
                IssueType.TASK, 3, TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM,
                "proj-1", "Core Infrastructure", "user-1", null, "user-reporter-1",
                List.of("updated"), LocalDate.now().plusDays(10), 0, Instant.now(), Instant.now()
        );

        when(taskService.updateTask(eq("task-123"), any(TaskRequest.class), anyString()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/tasks/task-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("PUT /api/tasks/{id} - Returns HTTP 404 for nonexistent task ID")
    void updateTask_Nonexistent_Returns404() throws Exception {
        TaskRequest updateRequest = new TaskRequest(
                "Valid Title", "Valid Description",
                IssueType.TASK, 3, TaskStatus.TODO, TaskPriority.LOW,
                "proj-1", null, null, null);

        when(taskService.updateTask(eq("nonexistent"), any(TaskRequest.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Task not found with id: nonexistent"));

        mockMvc.perform(put("/api/tasks/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/tasks/{id}
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("DELETE /api/tasks/{id} - Successfully deletes task and returns HTTP 204")
    void deleteTask_Existing_Returns204() throws Exception {
        doNothing().when(taskService).deleteTask(eq("task-123"), anyString());

        mockMvc.perform(delete("/api/tasks/task-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("DELETE /api/tasks/{id} - Returns HTTP 404 for nonexistent task ID")
    void deleteTask_Nonexistent_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found with id: invalid-id"))
                .when(taskService).deleteTask(eq("invalid-id"), anyString());

        mockMvc.perform(delete("/api/tasks/invalid-id"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/tasks/{id}/status
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("PATCH /api/tasks/{id}/status - Successfully changes status and returns HTTP 200")
    void updateStatus_Valid_Returns200() throws Exception {
        TaskResponse statusUpdated = new TaskResponse(
                "task-123", "CORE-101", "Automate Build Verification", "Description",
                IssueType.STORY, 5, TaskStatus.DONE, TaskPriority.HIGH,
                "proj-1", "Core Infrastructure", "user-1", null, "user-reporter-1",
                List.of("qa"), LocalDate.now().plusDays(5), 0, Instant.now(), Instant.now()
        );

        when(taskService.updateStatus(eq("task-123"), eq(TaskStatus.DONE), anyString()))
                .thenReturn(statusUpdated);

        mockMvc.perform(patch("/api/tasks/task-123/status")
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @WithMockUser(username = "tester@example.com")
    @DisplayName("PATCH /api/tasks/{id}/status - Rejects invalid / unsupported status with HTTP 400")
    void updateStatus_InvalidStatus_Returns400() throws Exception {
        mockMvc.perform(patch("/api/tasks/task-123/status")
                        .param("status", "INVALID_STATUS_VALUE"))
                .andExpect(status().isBadRequest());
    }
}
