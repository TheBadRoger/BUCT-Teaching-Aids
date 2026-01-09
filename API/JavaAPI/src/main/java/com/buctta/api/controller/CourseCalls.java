package com.buctta.api.controller;

import com.buctta.api.entities.CourseList;
import com.buctta.api.service.CourseService;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course")
public class CourseCalls {

    @Resource
    private CourseService courseService;

    @PostMapping("/add")
    public ResponseContainer<CourseList> addCourseCall(@RequestBody CourseList newCourse) {
        CourseList course = courseService.addCourse(newCourse);
        if (course != null) {
            return new ResponseContainer<>(0,"success",course);
        } else {

            return new ResponseContainer<>(0, "success", null);
        }
    }

    @GetMapping("/search")
    public ResponseContainer<Page<CourseList>> searchCourseCall(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String courseNumber,
            @RequestParam(required = false) String teachingTeachers,
            @RequestParam(required = false) String courseStatus,
            @RequestParam(required = false) String courseTags,
            @RequestParam(required = false) String startDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<CourseList> coursePage = courseService.searchCourses(
                    courseName, courseNumber, teachingTeachers, courseStatus, courseTags, startDate, pageable);

            return new ResponseContainer<>(0,"搜索成功",coursePage);

        } catch (Exception e) {
            System.err.println("搜索课程时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ResponseContainer<>(0, "搜索失败", null);
        }
    }
}