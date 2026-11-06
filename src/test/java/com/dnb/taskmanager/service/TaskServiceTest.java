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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectRepository, userRepository, commentRepository, userService);

        reporter = User.builder()
                .id("user-reporter")
                .email("reporter@example.com")
                .firstName("Bob")
                .lastName("Jones")
                .role(UserRole.DEVELOPER)
                .build();

        project = Project.builder()
                .id("proj-1")
                .name("Core Services")
                .build();
    }

    @Test
    @DisplayName("Should successfully create task")
    void createTask_Success() {
        TaskRequest request = new TaskRequest(
                "Implement Healthcheck", "Add Spring Actuator probes",
                IssueType.TASK, 5,
                TaskStatus.TODO, TaskPriority.HIGH, "proj-1", "user-reporter",
                List.of("backend", "actuator"), LocalDate.now().plusDays(5));

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(userRepository.existsById("user-reporter")).thenReturn(true);
        when(taskRepository.countByProjectId("proj-1")).thenReturn(0L);

        Task savedTask = Task.builder()
                .id("task-1")
                .title("Implement Healthcheck")
                .description("Add Spring Actuator probes")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .projectId("proj-1")
                .assigneeId("user-reporter")
                .reporterId("user-reporter")
                .labels(List.of("backend", "actuator"))
                .dueDate(request.dueDate())
                .createdAt(Instant.now())
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        when(commentRepository.countByTaskId("task-1")).thenReturn(0L);

        TaskResponse response = taskService.createTask(request, "reporter@example.com");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("task-1");
        assertThat(response.title()).isEqualTo("Implement Healthcheck");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should update task status successfully")
    void updateStatus_Success() {
        Task task = Task.builder()
                .id("task-1")
                .title("Design UI")
                .status(TaskStatus.TODO)
                .projectId("proj-1")
                .build();

        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(commentRepository.countByTaskId("task-1")).thenReturn(0L);

        TaskResponse response = taskService.updateStatus("task-1", TaskStatus.IN_PROGRESS, "user@example.com");

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when non-existent task requested")
    void getTaskById_NotFound() {
        when(taskRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
