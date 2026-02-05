package com.buctta.api.controller;

import com.buctta.api.entities.StudentCourse;
import com.buctta.api.service.StudentCourseService;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
public class StudentCourseCtrl {
    @Resource
    private StudentCourseService studentCourseService;

    @PostMapping("/select")
    public ResponseContainer<StudentCourse> selectCourseCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        try {
            StudentCourse studentCourse = studentCourseService.selectCourse(studentId, courseId);
            return new ResponseContainer<>(0, "选课成功", studentCourse);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1001, "选课失败: " + e.getMessage(), null);
        }
    }


    @PutMapping("/update-viewed")
    public ResponseContainer<StudentCourse> updateViewedStatusCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam Boolean isViewed) {
        try {
            StudentCourse studentCourse = studentCourseService.updateViewedStatus(studentId, courseId, isViewed);
            return new ResponseContainer<>(0, "状态更新成功", studentCourse);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1002, "状态更新失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/all-courses")
    public ResponseContainer<Page<StudentCourse>> getAllCoursesCall(
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
            return new ResponseContainer<>(0, "查询成功", courses);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1003, "查询失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/viewed-courses")
    public ResponseContainer<Page<StudentCourse>> getViewedCoursesCall(
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
            return new ResponseContainer<>(0, "查询成功", courses);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1004, "查询失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/not-viewed-courses")
    public ResponseContainer<Page<StudentCourse>> getNotViewedCoursesCall(
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
            return new ResponseContainer<>(0, "查询成功", courses);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1005, "查询失败: " + e.getMessage(), null);
        }
    }
    @DeleteMapping("/drop")
    public ResponseContainer<Void> dropCourseCall(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        try {
            studentCourseService.dropCourse(studentId, courseId);
            return new ResponseContainer<>(0, "退课成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1007, "退课失败: " + e.getMessage(), null);
        }
    }

}