package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 教参（教学参考资料）。
 * <p>
 * 与 {@link Note} 刻意分开：笔记是学生个人的学习沉淀，教参是教师按课程发布的参考资料，
 * 二者的权限与生命周期不同，混在一张表里会让查询与权限判断互相干扰。
 */
@Entity
@Table(name = "teaching_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeachingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /** 所属课程；可为空表示不绑定具体课程 */
    @Column(name = "course_id")
    private Long courseId;

    /** 发布教师（teacher_list.id）；可为空 */
    @Column(name = "teacher_id")
    private Long teacherId;

    /** 资料类型：讲义 / 习题 / 实验指导 / 参考书目 等，自由文本 */
    @Column(name = "material_type", length = 50)
    private String materialType;

    /** 附件地址，通常由 /api/media/upload 上传后回填 */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    /** 是否公开：true 表示学生可见 */
    @Column(name = "is_public", nullable = false,
            columnDefinition = "BIT(1) DEFAULT b'1'")
    private Boolean isPublic = true;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "created_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;

    @Column(name = "updated_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedTime;

    public TeachingMaterial(String title, String content, Long courseId, Long teacherId) {
        this.title = title;
        this.content = content;
        this.courseId = courseId;
        this.teacherId = teacherId;
    }
}
