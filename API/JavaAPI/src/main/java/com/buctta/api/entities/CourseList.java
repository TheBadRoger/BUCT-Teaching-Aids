package com.buctta.api.entities;

import jakarta.persistence.*;

@Table(name = "course_list")
@Entity
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

    public long getId() { return id; }
    public String getCourseName() { return courseName; }
    public String getCourseNumber() { return courseNumber; }
    public String getCourseIntroduction() { return courseIntroduction; }
    public String getStartDate() { return startDate; }
    public String getTeachingObjectives() { return teachingObjectives; }
    public String getDuration() { return duration; }
    public String getTeachingTeachers() { return teachingTeachers; }
    public String getTeachingClasses() { return teachingClasses; }
    public String getTargetAudience() { return targetAudience; }
    public String getClassAddress() { return classAddress; }
    public Double getCoursePrice() { return coursePrice; }
    public String getCourseStatus() { return courseStatus; }
    public String getCourseTags() { return courseTags; }
    public String getCourseOutline() { return courseOutline; }
    public String getCourseImage() { return courseImage; }
    public void setId(long id) { this.id = id; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setCourseNumber(String courseNumber) { this.courseNumber = courseNumber; }
    public void setCourseIntroduction(String courseIntroduction) { this.courseIntroduction = courseIntroduction; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setTeachingObjectives(String teachingObjectives) { this.teachingObjectives = teachingObjectives; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setTeachingTeachers(String teachingTeachers) { this.teachingTeachers = teachingTeachers; }
    public void setTeachingClasses(String teachingClasses) { this.teachingClasses = teachingClasses; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public void setClassAddress(String classAddress) { this.classAddress = classAddress; }
    public void setCoursePrice(Double coursePrice) { this.coursePrice = coursePrice; }
    public void setCourseStatus(String courseStatus) { this.courseStatus = courseStatus; }
    public void setCourseTags(String courseTags) { this.courseTags = courseTags; }
    public void setCourseOutline(String courseOutline) { this.courseOutline = courseOutline; }
    public void setCourseImage(String courseImage) { this.courseImage = courseImage; }
}