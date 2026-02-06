package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "course_list")
@Entity
@Getter
@Setter
public class CourseList {
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
    private Double coursePrice;
    private String courseStatus;
    private String courseTags;
    @Column(columnDefinition = "LONGTEXT")
    private String courseOutline;
    private String courseImage;

}