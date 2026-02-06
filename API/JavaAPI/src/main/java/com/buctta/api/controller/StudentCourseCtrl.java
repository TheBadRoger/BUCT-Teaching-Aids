package com.buctta.api.controller;

import com.buctta.api.entities.StudentCourse;
import com.buctta.api.service.StudentCourseService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
public class StudentCourseCtrl {
    @Resource
    private StudentCourseService studentCourseService;

    @PostMapping("/select")
    public ApiResponse<StudentCourse> selectCourseCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        try {
            StudentCourse studentCourse = studentCourseService.selectCourse(studentId, courseId);
            return ApiResponse.ok(studentCourse);
        }
        catch (Exception e) {
            log.error("选课时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

    @PutMapping("/update-viewed")
    public ApiResponse<StudentCourse> updateViewedStatusCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam Boolean isViewed) {
        try {
            StudentCourse studentCourse = studentCourseService.updateViewedStatus(studentId, courseId, isViewed);
            return ApiResponse.ok(studentCourse);
        }
        catch (Exception e) {
            log.error("更新课程查看状态时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

    @GetMapping("/all-courses")
    public ApiResponse<Page<StudentCourse>> getAllCoursesCall(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<StudentCourse> courses = studentCourseService.getAllCourses(studentId, pageable);
            return ApiResponse.ok(courses);
        }
        catch (Exception e) {
            log.error("获取所有课程时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

    @GetMapping("/viewed-courses")
    public ApiResponse<Page<StudentCourse>> getViewedCoursesCall(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<StudentCourse> courses = studentCourseService.getViewedCourses(studentId, pageable);
            return ApiResponse.ok(courses);
        }
        catch (Exception e) {
            log.error("获取已查看课程时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

    @GetMapping("/not-viewed-courses")
    public ApiResponse<Page<StudentCourse>> getNotViewedCoursesCall(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<StudentCourse> courses = studentCourseService.getNotViewedCourses(studentId, pageable);
            return ApiResponse.ok(courses);
        }
        catch (Exception e) {
            log.error("获取未查看课程时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/drop")
    public ApiResponse<Void> dropCourseCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        try {
            studentCourseService.dropCourse(studentId, courseId);
            return ApiResponse.ok(null);
        }
        catch (Exception e) {
            log.error("删除课程时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR.getCode(), "Failed: " + e.getMessage());
        }
    }

}