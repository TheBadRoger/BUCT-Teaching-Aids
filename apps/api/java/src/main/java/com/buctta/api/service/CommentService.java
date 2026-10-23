package com.buctta.api.service;

import com.buctta.api.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    // 添加评论
    Comment addComment(Comment comment);

    // 回复评论
    Comment replyComment(Long parentId, Comment comment);

    // 获取评论
    Comment getCommentById(Long id);

    // 删除评论
    boolean deleteComment(Long commentId, Long studentId);

    // 获取笔记的评论
    List<Comment> getCommentsByNote(Long noteId);

    // 分页获取笔记评论
    Page<Comment> getCommentsByNote(Long noteId, Pageable pageable);

    // 更新评论
    Comment updateComment(Long id, Comment commentDetails);

    // 获取评论的回复
    List<Comment> getRepliesByComment(Long parentId);
}