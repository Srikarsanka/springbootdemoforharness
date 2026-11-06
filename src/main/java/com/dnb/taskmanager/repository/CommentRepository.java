package com.dnb.taskmanager.repository;

import com.dnb.taskmanager.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByTaskId(String taskId);

    long countByTaskId(String taskId);
}
