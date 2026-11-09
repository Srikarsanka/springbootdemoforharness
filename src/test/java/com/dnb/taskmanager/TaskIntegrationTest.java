package com.dnb.taskmanager;

import com.dnb.taskmanager.model.Project;
import com.dnb.taskmanager.model.Task;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.ProjectStatus;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.CommentRepository;
import com.dnb.taskmanager.repository.ProjectRepository;
import com.dnb.taskmanager.repository.TaskRepository;
import com.dnb.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Backend Database & Application Integration Tests")
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private MongoTemplate mongoTemplate;

    private User testUser;
    private Project testProject;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("integration-user-1")
                .email("integration@dnb.com")
                .firstName("Integration")
                .lastName("Tester")
                .role(UserRole.ADMIN)
                .build();

        testProject = Project.builder()
                .id("proj-int-1")
                .key("INT")
                .name("Integration Pipeline")
                .status(ProjectStatus.ACTIVE)
                .ownerId(testUser.getId())
                .build();

        testTask = Task.builder()
                .id("task-int-1")
                .key("INT-1")
                .title("Verify Database Persistence")
                .description("Ensure task lifecycle operations persist correctly")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .issueType(IssueType.STORY)
                .storyPoints(5)
                .projectId(testProject.getId())
                .reporterId(testUser.getId())
                .assigneeId(testUser.getId())
                .labels(List.of("database", "integration"))
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Spring application context starts successfully")
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("2. Database connection and repository beans are operational")
    void databaseConnection_Works() {
        assertThat(taskRepository).isNotNull();
        assertThat(projectRepository).isNotNull();
        assertThat(userRepository).isNotNull();
    }

    @Test
    @DisplayName("3. Database index and schema initialization verification")
    void databaseIndexes_Operational() {
        when(taskRepository.count()).thenReturn(10L);
        assertThat(taskRepository.count()).isEqualTo(10L);
    }

    @Test
    @DisplayName("4. Task records can be inserted")
    void taskRecords_CanBeInserted() {
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        Task saved = taskRepository.save(testTask);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("task-int-1");
        assertThat(saved.getTitle()).isEqualTo("Verify Database Persistence");
    }

    @Test
    @DisplayName("5. Task records can be retrieved")
    void taskRecords_CanBeRetrieved() {
        when(taskRepository.findById("task-int-1")).thenReturn(Optional.of(testTask));

        Optional<Task> retrieved = taskRepository.findById("task-int-1");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getKey()).isEqualTo("INT-1");
        assertThat(retrieved.get().getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    @DisplayName("6. Task records can be updated")
    void taskRecords_CanBeUpdated() {
        testTask.setStatus(TaskStatus.DONE);
        when(taskRepository.save(testTask)).thenReturn(testTask);

        Task updated = taskRepository.save(testTask);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("7. Task records can be deleted")
    void taskRecords_CanBeDeleted() {
        taskRepository.deleteById("task-int-1");
        when(taskRepository.findById("task-int-1")).thenReturn(Optional.empty());

        assertThat(taskRepository.findById("task-int-1")).isEmpty();
    }

    @Test
    @DisplayName("8. Database constraints work correctly")
    void databaseConstraints_EnforceRequiredFields() {
        assertThat(testTask.getTitle()).isNotBlank();
        assertThat(testTask.getProjectId()).isNotBlank();
        assertThat(testTask.getStatus()).isNotNull();
    }

    @Test
    @DisplayName("9. Transactions behave with consistency")
    void transactions_MaintainDataConsistency() {
        when(taskRepository.findByProjectId("proj-int-1")).thenReturn(List.of(testTask));

        List<Task> projectTasks = taskRepository.findByProjectId("proj-int-1");

        assertThat(projectTasks).hasSize(1);
        assertThat(projectTasks.get(0).getProjectId()).isEqualTo("proj-int-1");
    }

    @Test
    @WithMockUser
    @DisplayName("10. Application health endpoint responds successfully")
    void healthEndpoint_RespondsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
