package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // Markdown内容

    @Column(columnDefinition = "TEXT")
    private String htmlContent; // 渲染后的HTML（可选）

    @Column(nullable = false)
    private String title;

    @Column(name = "is_public")
    private Boolean isPublic = false; // 是否公开

    @Column(name = "like_count")
    private Integer likeCount = 0; // 点赞数

    @Column(name = "comment_count")
    private Integer commentCount = 0; // 评论数

    // 关联学生
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 关联课程
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 点赞关联（多对多关系）
    @ManyToMany
    @JoinTable(
            name = "note_likes",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> likedByStudents = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 添加/移除点赞
    public boolean addLike(Student student) {
        if (likedByStudents.add(student)) {
            likeCount = likedByStudents.size();
            return true;
        }
        return false;
    }

    public boolean removeLike(Student student) {
        if (likedByStudents.remove(student)) {
            likeCount = likedByStudents.size();
            return true;
        }
        return false;
    }

    public boolean isLikedByStudent(Student student) {
        return likedByStudents.contains(student);
    }

    // 评论数量增减
    public void incrementCommentCount() {
        this.commentCount = (this.commentCount == null ? 0 : this.commentCount) + 1;
    }

    public void decrementCommentCount() {
        this.commentCount = Math.max(0, (this.commentCount == null ? 0 : this.commentCount) - 1);
    }
}