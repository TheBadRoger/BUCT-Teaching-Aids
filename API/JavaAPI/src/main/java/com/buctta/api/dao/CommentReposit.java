package com.buctta.api.dao;

import com.buctta.api.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReposit extends JpaRepository<Comment, Long> {

    // 根据笔记ID查找评论（按时间排序）
    List<Comment> findByNoteIdOrderByCreatedAtAsc(Long noteId);

    // 根据笔记ID查找评论（可分页）
    Page<Comment> findByNoteId(Long noteId, Pageable pageable);

    // 根据父评论ID查找回复
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    // 根据学生ID查找评论
    Page<Comment> findByStudentId(Long studentId, Pageable pageable);

    // 统计笔记的评论数量
    Long countByNoteId(Long noteId);

    // 查找评论及其回复
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.parentComment WHERE c.note.id = :noteId ORDER BY c.createdAt ASC")
    List<Comment> findCommentsWithReplies(@Param("noteId") Long noteId);

    void deleteByNoteId(Long noteId);
}