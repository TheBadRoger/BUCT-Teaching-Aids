package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 单个学生对单个视频的学习记录（逐视频轨迹）。
 * <p>
 * 与 {@link StudentCourse} 的分工：后者是**课程级**的续播指针（最近看到哪个视频、哪一秒），
 * 本实体是**视频级**的明细（每个视频看了多久、是否看完）。课程级指针只能记住一个位置，
 * 无法回答"这门课里哪些视频看完了"。
 * <p>
 * {@code (student_id, video_id)} 唯一：同一学生对同一视频只有一条记录，重复上报做累加与覆盖。
 */
@Entity
@Table(name = "student_video_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_video_progress",
                columnNames = {"student_id", "video_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentVideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    /**
     * 累计有效观看秒数。
     * <p>
     * 由上报的播放区间合并去重后累加，因此来回拖动进度条不会虚增。
     */
    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds = 0;

    /** 最近播放位置（秒），用于该视频内部的续播 */
    @Column(name = "last_position", nullable = false)
    private Integer lastPosition = 0;

    /** 是否已看完该视频 */
    @Column(name = "completed", nullable = false,
            columnDefinition = "BIT(1) DEFAULT b'0'")
    private Boolean completed = false;

    @Column(name = "first_watched_at", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime firstWatchedAt;

    @Column(name = "updated_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedTime;

    public StudentVideoProgress(Long studentId, Long courseId, Long videoId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.videoId = videoId;
    }
}
