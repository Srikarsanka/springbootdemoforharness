package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.TaskRequest;
import com.dnb.taskmanager.dto.response.TaskResponse;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.model.Project;
import com.dnb.taskmanager.model.Task;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.CommentRepository;
import com.dnb.taskmanager.repository.ProjectRepository;
import com.dnb.taskmanager.repository.TaskRepository;
import com.dnb.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    private TaskService taskService;

    private User reporter;
    private Project project;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectRepository, userRepository, commentRepository, userService);

        reporter = User.builder()
                .id("user-reporter-1")
                .email("reporter@example.com")
                .firstName("Bob")
                .lastName("Jones")
                .role(UserRole.DEVELOPER)
                .build();

        project = Project.builder()
                .id("proj-1")
                .key("CORE")
                .name("Core Infrastructure")
                .build();

        existingTask = Task.builder()
                .id("task-101")
                .key("CORE-1")
                .title("Initial Task")
                .description("Initial Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .projectId("proj-1")
                .assigneeId("user-reporter-1")
                .reporterId("user-reporter-1")
                .issueType(IssueType.STORY)
                .storyPoints(3)
                .labels(List.of("backend"))
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. Successfully creating a task
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. Successfully creating a task")
    void createTask_Success() {
        TaskRequest request = new TaskRequest(
                "Build CI Pipeline", "Implement build verification gates",
                IssueType.TASK, 5, TaskStatus.TODO, TaskPriority.HIGH,
                "proj-1", "user-reporter-1", List.of("ci", "devops"), LocalDate.now().plusDays(3));

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(userRepository.existsById("user-reporter-1")).thenReturn(true);
        when(taskRepository.countByProjectId("proj-1")).thenReturn(0L);

        Task saved = Task.builder()
                .id("task-new")
                .key("CORE-1")
                .title("Build CI Pipeline")
                .description("Implement build verification gates")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .projectId("proj-1")
                .assigneeId("user-reporter-1")
                .reporterId("user-reporter-1")
                .issueType(IssueType.TASK)
                .storyPoints(5)
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(request, "reporter@example.com");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("task-new");
        assertThat(response.key()).isEqualTo("CORE-1");
        assertThat(response.title()).isEqualTo("Build CI Pipeline");
        verify(taskRepository).save(any(Task.class));
    }

    // -------------------------------------------------------------------------
    // 2. Creating a task with valid data
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Creating a task with valid data sets all properties")
    void createTask_WithValidData_SetsAllProperties() {
        TaskRequest request = new TaskRequest(
                "Secure API with JWT", "Enforce Bearer token auth",
                IssueType.BUG, 8, TaskStatus.IN_PROGRESS, TaskPriority.CRITICAL,
                "proj-1", "user-reporter-1", List.of("security"), LocalDate.now().plusDays(7));

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(userRepository.existsById("user-reporter-1")).thenReturn(true);
        when(taskRepository.countByProjectId("proj-1")).thenReturn(2L);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.createTask(request, "reporter@example.com");

        assertThat(response.title()).isEqualTo("Secure API with JWT");
        assertThat(response.issueType()).isEqualTo(IssueType.BUG);
        assertThat(response.storyPoints()).isEqualTo(8);
        assertThat(response.priority()).isEqualTo(TaskPriority.CRITICAL);
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    // -------------------------------------------------------------------------
    // 3. Retrieving all tasks
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Retrieving all tasks")
    void getAllTasks_ReturnsAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(existingTask));

        List<TaskResponse> responses = taskService.getAllTasks(null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Initial Task");
    }

    // -------------------------------------------------------------------------
    // 4. Retrieving a task using a valid ID
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Retrieving a task using a valid ID")
    void getTaskById_ValidId_ReturnsTask() {
        when(taskRepository.findById("task-101")).thenReturn(Optional.of(existingTask));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));

        TaskResponse response = taskService.getTaskById("task-101");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("task-101");
        assertThat(response.title()).isEqualTo("Initial Task");
    }

    // -------------------------------------------------------------------------
    // 5. Handling a task ID that does not exist
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Handling a task ID that does not exist throws ResourceNotFoundException")
    void getTaskById_NotFound_ThrowsException() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: nonexistent");
    }

    // -------------------------------------------------------------------------
    // 6. Updating an existing task
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Updating an existing task")
    void updateTask_Success() {
        TaskRequest updateRequest = new TaskRequest(
                "Updated Task Title", "Updated Description",
                IssueType.EPIC, 13, TaskStatus.IN_REVIEW, TaskPriority.HIGH,
                "proj-1", "user-reporter-1", List.of("updated"), LocalDate.now().plusDays(10));

        when(taskRepository.findById("task-101")).thenReturn(Optional.of(existingTask));
        when(userRepository.existsById("user-reporter-1")).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask("task-101", updateRequest, "reporter@example.com");

        assertThat(response.title()).isEqualTo("Updated Task Title");
        assertThat(response.description()).isEqualTo("Updated Description");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_REVIEW);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.storyPoints()).isEqualTo(13);
    }

    // -------------------------------------------------------------------------
    // 7. Handling updates for a nonexistent task
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Handling updates for a nonexistent task throws ResourceNotFoundException")
    void updateTask_NotFound_ThrowsException() {
        TaskRequest updateRequest = new TaskRequest(
                "Updated Task Title", "Updated Description",
                null, null, null, null, null, null, null, null);

        when(taskRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask("unknown-id", updateRequest, "reporter@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // 8. Deleting an existing task
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Deleting an existing task")
    void deleteTask_Success() {
        when(taskRepository.existsById("task-101")).thenReturn(true);

        taskService.deleteTask("task-101", "reporter@example.com");

        verify(taskRepository).deleteById("task-101");
    }

    // -------------------------------------------------------------------------
    // 9. Handling deletion of a nonexistent task
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Handling deletion of a nonexistent task throws ResourceNotFoundException")
    void deleteTask_NotFound_ThrowsException() {
        when(taskRepository.existsById("invalid-task-id")).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask("invalid-task-id", "reporter@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: invalid-task-id");

        verify(taskRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // 10. Updating task status
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Updating task status")
    void updateStatus_Success() {
        when(taskRepository.findById("task-101")).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus("task-101", TaskStatus.DONE, "reporter@example.com");

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository).save(existingTask);
    }

    // -------------------------------------------------------------------------
    // 11. Filtering tasks by status
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. Filtering tasks by status")
    void getTasksByStatus_FiltersCorrectly() {
        when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(List.of(existingTask));

        List<TaskResponse> results = taskService.getAllTasks(null, TaskStatus.TODO, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(TaskStatus.TODO);
    }

    // -------------------------------------------------------------------------
    // 12. Filtering tasks by priority
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("12. Filtering tasks by priority")
    void getTasksByPriority_FiltersCorrectly() {
        when(taskRepository.findByPriority(TaskPriority.MEDIUM)).thenReturn(List.of(existingTask));

        List<TaskResponse> results = taskService.getAllTasks(null, null, TaskPriority.MEDIUM, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    // -------------------------------------------------------------------------
    // 13. Searching tasks by title
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13. Searching tasks by title")
    void searchTasksByTitle_ReturnsMatchingTasks() {
        when(taskRepository.findByTitleContainingIgnoreCase("Initial")).thenReturn(List.of(existingTask));

        List<TaskResponse> results = taskService.getAllTasks("Initial", null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).contains("Initial");
    }

    // -------------------------------------------------------------------------
    // 14. Handling empty search results
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("14. Handling empty search results")
    void searchTasksByTitle_EmptyResults_ReturnsEmptyList() {
        when(taskRepository.findByTitleContainingIgnoreCase("NonExistentTerm")).thenReturn(Collections.emptyList());

        List<TaskResponse> results = taskService.getAllTasks("NonExistentTerm", null, null, null);

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 15. Validating required fields
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("15. Validating required fields throws exception when project does not exist")
    void createTask_MissingProject_ThrowsException() {
        TaskRequest request = new TaskRequest(
                "Task with missing project", null, null, null, null, null,
                "missing-proj-id", null, null, null);

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("missing-proj-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request, "reporter@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found with id: missing-proj-id");
    }

    // -------------------------------------------------------------------------
    // 16. Handling invalid task status update
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("16. Handling invalid task status update on nonexistent task")
    void updateStatus_NonExistentTask_ThrowsException() {
        when(taskRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus("unknown-id", TaskStatus.DONE, "reporter@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // 17. Handling default priority when omitted
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("17. Handling omitted priority falls back to default MEDIUM")
    void createTask_DefaultPriority_WhenNull() {
        TaskRequest request = new TaskRequest(
                "Task Without Priority", "Description",
                IssueType.TASK, 3, TaskStatus.TODO, null,
                "proj-1", null, null, null);

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId("proj-1")).thenReturn(0L);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.createTask(request, "reporter@example.com");

        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    // -------------------------------------------------------------------------
    // 18. Handling unexpected repository exceptions
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("18. Handling unexpected repository exceptions")
    void createTask_RepositoryException_PropagatesException() {
        TaskRequest request = new TaskRequest(
                "Task That Fails DB", null, null, null, null, null,
                "proj-1", null, null, null);

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId("proj-1")).thenReturn(0L);
        doThrow(new RuntimeException("MongoDB connection timeout")).when(taskRepository).save(any(Task.class));

        assertThatThrownBy(() -> taskService.createTask(request, "reporter@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MongoDB connection timeout");
    }
}
