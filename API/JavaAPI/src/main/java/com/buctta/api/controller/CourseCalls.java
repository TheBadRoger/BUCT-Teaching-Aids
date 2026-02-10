package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j

@RestController
@RequestMapping("/api/course")
public class CourseCalls {

    @Resource
    private CourseService courseService;

    @PostMapping("/add")
    public ApiResponse<Course> addCourseCall(@RequestBody Course newCourse) {
        Course course = courseService.addCourse(newCourse);
        if (course != null) {
            return ApiResponse.ok(course);
        }
        else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS);
        }
    }

    @GetMapping("/search")
    public ApiResponse<Page<Course>> searchCourseCall(
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
            Page<Course> coursePage = courseService.searchCourses(
                    courseName, courseNumber, teachingTeachers, courseStatus, courseTags, startDate, pageable);

            return ApiResponse.ok(coursePage);

        }
        catch (Exception e) {
            log.error("搜索课程时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}
