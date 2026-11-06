package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.TaskRequest;
import com.dnb.taskmanager.dto.response.TaskResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.model.Project;
import com.dnb.taskmanager.model.Task;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.IssueType;
import com.dnb.taskmanager.model.enums.TaskPriority;
import com.dnb.taskmanager.model.enums.TaskStatus;
import com.dnb.taskmanager.repository.CommentRepository;
import com.dnb.taskmanager.repository.ProjectRepository;
import com.dnb.taskmanager.repository.TaskRepository;
import com.dnb.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       CommentRepository commentRepository,
                       UserService userService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
    }

    public TaskResponse createTask(TaskRequest request, String userEmail) {
        User reporter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.projectId()));

        if (request.assigneeId() != null && !request.assigneeId().isBlank()) {
            if (!userRepository.existsById(request.assigneeId())) {
                throw new ResourceNotFoundException("Assignee not found with id: " + request.assigneeId());
            }
        }

        TaskStatus status = request.status() != null ? request.status() : TaskStatus.BACKLOG;
        TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;
        List<String> labels = request.labels() != null ? new ArrayList<>(request.labels()) : new ArrayList<>();

        long count = taskRepository.countByProjectId(project.getId()) + 1;
        String projKey = project.getKey() != null ? project.getKey() : "PRJ";
        String taskKey = projKey + "-" + count;

        IssueType issueType = request.issueType() != null ? request.issueType() : IssueType.STORY;
        Integer storyPoints = request.storyPoints() != null ? request.storyPoints() : 3;

        Task task = Task.builder()
                .key(taskKey)
                .title(request.title().trim())
                .description(request.description())
                .issueType(issueType)
                .storyPoints(storyPoints)
                .status(status)
                .priority(priority)
                .projectId(project.getId())
                .assigneeId(request.assigneeId())
                .reporterId(reporter.getId())
                .labels(labels)
                .dueDate(request.dueDate())
                .build();

        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    public Task getTaskEntityById(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public TaskResponse getTaskById(String id) {
        return toTaskResponse(getTaskEntityById(id));
    }

    public List<TaskResponse> getTasksByProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    public List<TaskResponse> getMyTasks(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return taskRepository.findByAssigneeId(user.getId()).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    public List<TaskResponse> getTasksByProjectAndStatus(String projectId, TaskStatus status) {
        return taskRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    public TaskResponse updateTask(String id, TaskRequest request, String userEmail) {
        Task task = getTaskEntityById(id);

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.issueType() != null) {
            task.setIssueType(request.issueType());
        }
        if (request.storyPoints() != null) {
            task.setStoryPoints(request.storyPoints());
        }
        if (request.assigneeId() != null) {
            if (!request.assigneeId().isBlank() && !userRepository.existsById(request.assigneeId())) {
                throw new ResourceNotFoundException("Assignee not found with id: " + request.assigneeId());
            }
            task.setAssigneeId(request.assigneeId().isBlank() ? null : request.assigneeId());
        }
        if (request.labels() != null) {
            task.setLabels(new ArrayList<>(request.labels()));
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    public TaskResponse updateStatus(String id, TaskStatus newStatus, String userEmail) {
        Task task = getTaskEntityById(id);
        task.setStatus(newStatus);
        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    public void deleteTask(String id, String userEmail) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    public TaskResponse toTaskResponse(Task task) {
        if (task == null) {
            return null;
        }

        String projectName = projectRepository.findById(task.getProjectId())
                .map(Project::getName)
                .orElse("Unknown Project");

        UserResponse assigneeResponse = null;
        if (task.getAssigneeId() != null && !task.getAssigneeId().isBlank()) {
            assigneeResponse = userRepository.findById(task.getAssigneeId())
                    .map(userService::toUserResponse)
                    .orElse(null);
        }

        int commentCount = (int) commentRepository.countByTaskId(task.getId());

        String taskKey = task.getKey();
        if (taskKey == null || taskKey.isBlank()) {
            taskKey = "ISSUE-" + (task.getId() != null ? task.getId().substring(Math.max(0, task.getId().length() - 4)) : "1");
        }

        return new TaskResponse(
                task.getId(),
                taskKey,
                task.getTitle(),
                task.getDescription(),
                task.getIssueType() != null ? task.getIssueType() : IssueType.STORY,
                task.getStoryPoints() != null ? task.getStoryPoints() : 3,
                task.getStatus(),
                task.getPriority(),
                task.getProjectId(),
                projectName,
                task.getAssigneeId(),
                assigneeResponse,
                task.getReporterId(),
                task.getLabels() != null ? task.getLabels() : List.of(),
                task.getDueDate(),
                commentCount,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
