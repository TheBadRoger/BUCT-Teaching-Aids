package com.buctta.api.dao;

import com.buctta.api.entities.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteReposit extends JpaRepository<Note, Long> {

        // 根据课程ID查找笔记（可分页）
        Page<Note> findByCourseId(Long courseId, Pageable pageable);

        // 根据学生ID查找笔记（可分页）
        Page<Note> findByStudentId(Long studentId, Pageable pageable);

        // 根据课程ID和学生ID查找笔记（可分页）
        Page<Note> findByCourseIdAndStudentId(Long courseId, Long studentId, Pageable pageable);

        // 查找公开笔记
        Page<Note> findByIsPublicTrue(Pageable pageable);

        // 根据课程ID查找公开笔记
        Page<Note> findByCourseIdAndIsPublicTrue(Long courseId, Pageable pageable);

        // 搜索笔记（标题和内容）
        @Query("SELECT n FROM Note n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword% AND n.isPublic = true")
        Page<Note> searchPublicNotes(@Param("keyword") String keyword, Pageable pageable);

        // 检查学生是否已经点赞
        @Query("SELECT COUNT(1) > 0 FROM Note n JOIN n.likedByStudents s WHERE n.id = :noteId AND s.id = :studentId")
        boolean existsLike(@Param("noteId") Long noteId, @Param("studentId") Long studentId);

        // 更新点赞数
        @Modifying
        @Query("UPDATE Note n SET n.likeCount = n.likeCount + :increment WHERE n.id = :noteId")
        void updateLikeCount(@Param("noteId") Long noteId, @Param("increment") int increment);

        // 查找热门笔记（按点赞数排序）
        Page<Note> findByIsPublicTrueOrderByLikeCountDesc(Pageable pageable);

        // 查找最新笔记
        Page<Note> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
}