package com.buctta.api.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourse {
    @EmbeddedId
    private StudentCourseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(
            name = "is_viewed",
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT false"
    )
    private Boolean isViewed = false;

    /** 最近播放的视频，指向 course_video.id，用于断点续播 */
    @Column(name = "last_video_id")
    private Long lastVideoId;

    /** 最近播放位置（秒） */
    @Column(name = "last_position_seconds", nullable = false,
            columnDefinition = "INT DEFAULT 0")
    private Integer lastPositionSeconds = 0;

    public StudentCourse(Student student, Course course) {
        this.student = student;
        this.course = course;
        this.id = new StudentCourseId(student.getId(), course.getId());
    }
}