package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.CommentRequest;
import com.dnb.taskmanager.dto.response.CommentResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.exception.UnauthorizedException;
import com.dnb.taskmanager.model.Comment;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.CommentRepository;
import com.dnb.taskmanager.repository.TaskRepository;
import com.dnb.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository,
                          TaskRepository taskRepository,
                          UserRepository userRepository,
                          UserService userService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public CommentResponse addComment(CommentRequest request, String userEmail) {
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!taskRepository.existsById(request.taskId())) {
            throw new ResourceNotFoundException("Task not found with id: " + request.taskId());
        }

        Comment comment = Comment.builder()
                .content(request.content().trim())
                .taskId(request.taskId())
                .authorId(author.getId())
                .build();

        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    public List<CommentResponse> getCommentsByTaskId(String taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        return commentRepository.findByTaskId(taskId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    public void deleteComment(String commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!comment.getAuthorId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }

        commentRepository.deleteById(commentId);
    }

    public CommentResponse toCommentResponse(Comment comment) {
        if (comment == null) {
            return null;
        }

        User author = userRepository.findById(comment.getAuthorId()).orElse(null);
        UserResponse authorResponse = userService.toUserResponse(author);

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getTaskId(),
                comment.getAuthorId(),
                authorResponse,
                comment.getCreatedAt()
        );
    }
}

