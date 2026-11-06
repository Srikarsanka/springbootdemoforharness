package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.ProjectRequest;
import com.dnb.taskmanager.dto.response.ProjectResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.exception.UnauthorizedException;
import com.dnb.taskmanager.model.Project;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.ProjectStatus;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.ProjectRepository;
import com.dnb.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private ProjectService projectService;

    private User owner;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userRepository, userService);

        owner = User.builder()
                .id("user-1")
                .email("owner@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(UserRole.PROJECT_MANAGER)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new project")
    void createProject_Success() {
        ProjectRequest request = new ProjectRequest(
                "Cloud Migration", "MIG", "Migrate legacy app to containerized architecture",
                ProjectStatus.ACTIVE, List.of("user-2"),
                LocalDate.now(), LocalDate.now().plusMonths(3));

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        Project savedProject = Project.builder()
                .id("proj-101")
                .name("Cloud Migration")
                .description("Migrate legacy app to containerized architecture")
                .status(ProjectStatus.ACTIVE)
                .ownerId("user-1")
                .memberIds(List.of("user-2", "user-1"))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .createdAt(Instant.now())
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(owner));
        when(userService.toUserResponse(owner)).thenReturn(
                new UserResponse("user-1", "owner@example.com", "Alice", "Smith", UserRole.PROJECT_MANAGER, Instant.now()));

        ProjectResponse response = projectService.createProject(request, "owner@example.com");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("proj-101");
        assertThat(response.name()).isEqualTo("Cloud Migration");
        assertThat(response.memberCount()).isEqualTo(2);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-owner updates project")
    void updateProject_Unauthorized() {
        Project project = Project.builder()
                .id("proj-1")
                .name("Secret Project")
                .ownerId("user-1")
                .memberIds(new ArrayList<>(List.of("user-1")))
                .build();

        User attacker = User.builder()
                .id("user-999")
                .email("other@example.com")
                .role(UserRole.DEVELOPER)
                .build();

        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(attacker));

        ProjectRequest request = new ProjectRequest("Renamed", "REN", "Desc", ProjectStatus.ACTIVE, null, null, null);

        assertThatThrownBy(() -> projectService.updateProject("proj-1", request, "other@example.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when project not found")
    void getProjectById_NotFound() {
        when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
