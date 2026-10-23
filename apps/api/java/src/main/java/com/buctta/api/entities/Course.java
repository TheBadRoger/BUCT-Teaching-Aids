package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Table(name = "course_list")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false, unique = true)
    private String courseNumber;

    @Column(columnDefinition = "TEXT")
    private String courseIntroduction;

    private String startDate;
    private String teachingObjectives;
    private String duration;
    private String teachingTeachers;
    private String teachingClasses;
    private String targetAudience;
    private String classAddress;

    /**
     * 课程价格。
     * <p>
     * 数据库列是 {@code DECIMAL(10,2)}（金额不该用浮点），而 Java 侧是 {@code Double}。
     * Hibernate 默认会把 {@code Double} 期望成 {@code FLOAT}，导致 ddl-auto=validate
     * 报 "found [decimal], but expecting [float(53)]"，因此这里用 columnDefinition
     * 显式声明为 DECIMAL 让两边对齐（比把列改成 DOUBLE 更安全：不引入金额精度误差）。
     */
    @Column(name = "course_price", columnDefinition = "DECIMAL(10,2)")
    private Double coursePrice;

    private String courseStatus;
    private String courseTags;
    @Column(columnDefinition = "LONGTEXT")
    private String courseOutline;
    private String courseImage;

    // ...existing code...
    /**
     * 浏览次数。
     * <p>
     * 数据库列是 {@code INT}，而这里是 {@code Long}（Hibernate 会期望 BIGINT），
     * 因此用 columnDefinition 显式声明，避免 ddl-auto=validate 报类型不符。
     * 计数器保留 INT 即可满足容量，也无需为此涨列宽。
     */
    @Column(name = "view_count", nullable = false, columnDefinition = "INT")
    private Long viewCount = 0L;

    /**
     * 是否已发布。
     * <p>
     * 与自由文本的 {@link #courseStatus} 分开：后者描述"进行中/已结课"这类教学状态，
     * 不适合承载"是否通过开课审核"。教师提交开课申请并审核通过后才会置为 true。
     */
    @Column(name = "published", nullable = false,
            columnDefinition = "BIT(1) DEFAULT b'0'")
    private Boolean published = false;


}

