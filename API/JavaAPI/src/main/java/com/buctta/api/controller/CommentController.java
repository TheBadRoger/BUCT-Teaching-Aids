package com.buctta.api.controller;

import com.buctta.api.entities.Comment;
import com.buctta.api.service.CommentService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 添加评论
     */
    @PostMapping("/add")
    public ApiResponse<Comment> addComment(@RequestBody Comment comment) {
        try {
            Comment createdComment = commentService.addComment(comment);
            return ApiResponse.ok(createdComment);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("添加评论异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 回复评论（指定父评论ID）
     */
    @PostMapping("/reply/{parentId}")
    public ApiResponse<Comment> replyComment(
            @PathVariable Long parentId,
            @RequestBody Comment comment) {
        try {
            Comment reply = commentService.replyComment(parentId, comment);
            return ApiResponse.ok(reply);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("回复评论异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 删除评论（仅作者可操作）
     */
    @DeleteMapping("/{commentId}")
    public ApiResponse<Boolean> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long studentId) {
        try {
            boolean result = commentService.deleteComment(commentId, studentId);
            return ApiResponse.ok(result);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (SecurityException e) {
            return ApiResponse.fail(BusinessStatus.NO_PERMISSION);
        } catch (Exception e) {
            log.error("删除评论异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取指定笔记的所有评论（含回复，树形结构由前端处理，此处返回平铺列表）
     */
    @GetMapping("/note/{noteId}")
    public ApiResponse<List<Comment>> getCommentsByNote(@PathVariable Long noteId) {
        try {
            List<Comment> comments = commentService.getCommentsByNote(noteId);
            return ApiResponse.ok(comments);
        } catch (Exception e) {
            log.error("获取评论列表异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 分页获取笔记的评论（一级评论，不包含回复）
     */
    @GetMapping("/note/{noteId}/page")
    public ApiResponse<Page<Comment>> getCommentsByNotePage(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
            Page<Comment> comments = commentService.getCommentsByNote(noteId, pageable);
            return ApiResponse.ok(comments);
        } catch (Exception e) {
            log.error("分页获取评论异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取单条评论详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Comment> getComment(@PathVariable Long id) {
        try {
            Comment comment = commentService.getCommentById(id);
            return ApiResponse.ok(comment);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (Exception e) {
            log.error("获取评论详情异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 更新评论（仅作者可操作）
     */
    @PutMapping("/{id}")
    public ApiResponse<Comment> updateComment(
            @PathVariable Long id,
            @RequestBody Comment commentDetails) {
        try {
            Comment updatedComment = commentService.updateComment(id, commentDetails);
            return ApiResponse.ok(updatedComment);
        } catch (EntityNotFoundException e) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        } catch (SecurityException e) {
            return ApiResponse.fail(BusinessStatus.NO_PERMISSION);
        } catch (Exception e) {
            log.error("更新评论异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取某条评论下的所有回复
     */
    @GetMapping("/{parentId}/replies")
    public ApiResponse<List<Comment>> getReplies(@PathVariable Long parentId) {
        try {
            List<Comment> replies = commentService.getRepliesByComment(parentId);
            return ApiResponse.ok(replies);
        } catch (Exception e) {
            log.error("获取回复列表异常: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}