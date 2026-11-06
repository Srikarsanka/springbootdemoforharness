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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          UserService userService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public ProjectResponse createProject(ProjectRequest request, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<String> members = new ArrayList<>();
        if (request.memberIds() != null) {
            members.addAll(request.memberIds());
        }
        if (!members.contains(owner.getId())) {
            members.add(owner.getId());
        }

        ProjectStatus status = request.status() != null ? request.status() : ProjectStatus.PLANNING;

        String key = request.key();
        if (key == null || key.isBlank()) {
            String nameClean = request.name().replaceAll("[^a-zA-Z]", "").toUpperCase();
            key = nameClean.length() >= 3 ? nameClean.substring(0, Math.min(4, nameClean.length())) : "PRJ";
        } else {
            key = key.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        }

        Project project = Project.builder()
                .key(key)
                .name(request.name().trim())
                .description(request.description())
                .status(status)
                .ownerId(owner.getId())
                .memberIds(members)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        Project saved = projectRepository.save(project);
        return toProjectResponse(saved);
    }

    public Project getProjectEntityById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public ProjectResponse getProjectById(String id) {
        return toProjectResponse(getProjectEntityById(id));
    }

    public List<ProjectResponse> getProjectsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getRole() == UserRole.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(this::toProjectResponse)
                    .toList();
        }

        List<Project> owned = projectRepository.findByOwnerId(user.getId());
        List<Project> memberOf = projectRepository.findByMemberIdsContaining(user.getId());

        Set<String> seenIds = new HashSet<>();
        List<Project> combined = new ArrayList<>();

        for (Project p : owned) {
            if (seenIds.add(p.getId())) {
                combined.add(p);
            }
        }
        for (Project p : memberOf) {
            if (seenIds.add(p.getId())) {
                combined.add(p);
            }
        }

        return combined.stream()
                .map(this::toProjectResponse)
                .toList();
    }

    public ProjectResponse updateProject(String id, ProjectRequest request, String userEmail) {
        Project project = getProjectEntityById(id);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!project.getOwnerId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to update this project");
        }

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            project.setEndDate(request.endDate());
        }
        if (request.memberIds() != null) {
            List<String> updatedMembers = new ArrayList<>(request.memberIds());
            if (!updatedMembers.contains(project.getOwnerId())) {
                updatedMembers.add(project.getOwnerId());
            }
            project.setMemberIds(updatedMembers);
        }

        Project saved = projectRepository.save(project);
        return toProjectResponse(saved);
    }

    public void deleteProject(String id, String userEmail) {
        Project project = getProjectEntityById(id);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!project.getOwnerId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to delete this project");
        }

        projectRepository.deleteById(id);
    }

    public ProjectResponse addMember(String projectId, String memberId, String userEmail) {
        Project project = getProjectEntityById(projectId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!project.getOwnerId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only project owners or admins can add members");
        }

        if (!userRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("User not found with id: " + memberId);
        }

        List<String> members = project.getMemberIds();
        if (members == null) {
            members = new ArrayList<>();
        }
        if (!members.contains(memberId)) {
            members.add(memberId);
            project.setMemberIds(members);
            project = projectRepository.save(project);
        }

        return toProjectResponse(project);
    }

    public ProjectResponse removeMember(String projectId, String memberId, String userEmail) {
        Project project = getProjectEntityById(projectId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!project.getOwnerId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only project owners or admins can remove members");
        }

        if (project.getOwnerId().equals(memberId)) {
            throw new IllegalArgumentException("Cannot remove the project owner from project members");
        }

        List<String> members = project.getMemberIds();
        if (members != null && members.remove(memberId)) {
            project.setMemberIds(members);
            project = projectRepository.save(project);
        }

        return toProjectResponse(project);
    }

    public ProjectResponse toProjectResponse(Project project) {
        if (project == null) {
            return null;
        }

        User owner = userRepository.findById(project.getOwnerId()).orElse(null);
        UserResponse ownerResponse = userService.toUserResponse(owner);
        int memberCount = project.getMemberIds() != null ? project.getMemberIds().size() : 0;

        return new ProjectResponse(
                project.getId(),
                project.getKey() != null ? project.getKey() : "PRJ",
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getOwnerId(),
                ownerResponse,
                project.getMemberIds() != null ? project.getMemberIds() : List.of(),
                memberCount,
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
