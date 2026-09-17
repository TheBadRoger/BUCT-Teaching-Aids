package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 课程下的一个视频（课程 → 视频 一对多）。
 * <p>
 * 一门课程可以挂多个视频，按 {@link #sortOrder} 升序排列即为 MOOC 式的视频目录。
 * {@link #videoFileId} 与 {@link #coverFileId} 逻辑外键指向 {@link MediaFile#getId()}，
 * 这里刻意不用 {@code @ManyToOne}，避免与既有的扁平实体风格不一致。
 */
@Entity
@Table(name = "course_video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 课程内播放顺序，越小越靠前 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 视频时长（秒），由前端读取媒体元数据后回填，可为空 */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** 视频文件，指向 media_file.id */
    @Column(name = "video_file_id", nullable = false)
    private Long videoFileId;

    /** 封面图，指向 media_file.id，可为空 */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    @Column(name = "created_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;

    @Column(name = "updated_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedTime;

    public CourseVideo(Long courseId, String title, Long videoFileId) {
        this.courseId = courseId;
        this.title = title;
        this.videoFileId = videoFileId;
    }
}
