package com.buctta.api.controller;

import com.buctta.api.entities.CourseList;
import com.buctta.api.service.CourseService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
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
    public ApiResponse<CourseList> addCourseCall(@RequestBody CourseList newCourse) {
        CourseList course = courseService.addCourse(newCourse);
        if (course != null) {
            return ApiResponse.ok(course);
        }
        else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS);
        }
    }

    @GetMapping("/search")
    public ApiResponse<Page<CourseList>> searchCourseCall(
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

            return ApiResponse.ok(coursePage);

        } catch (Exception e) {
            System.err.println("搜索课程时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail(500, "Failed");
        }
    }
}