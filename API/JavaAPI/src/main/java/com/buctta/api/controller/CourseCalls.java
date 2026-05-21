package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j

@RestController
@RequestMapping("/api/course")
public class CourseCalls {

    @Resource
    private CourseService courseService;

    @PostMapping("/add")
    public ApiResponse<Course> addCourseCall(@RequestBody Course newCourse) {
        CourseService.CourseResult result = courseService.addCourse(newCourse);
        if (result.success()) {
            return ApiResponse.ok(result.course());
        }
        else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS, result.message());
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
    // 批量删除
    @DeleteMapping("/batch")
    public ApiResponse<String> deleteCourses(@RequestBody List<Long> ids) {
        CourseService.CourseResult result = courseService.deleteCourses(ids);
        if (result.success()) {
            return ApiResponse.ok(result.message());
        } else {
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, result.message());
        }
    }

    // 导出Excel
    @GetMapping("/export")
    public void exportCourses(HttpServletResponse response) throws IOException {
        List<Course> courses = courseService.getAllCourses();
        byte[] excelBytes = courseService.exportCoursesToExcel(courses);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=courses.xlsx");
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
    /**
     * 编辑课程信息
     */
    @PutMapping("/update")
    public ApiResponse<Course> updateCourse(@RequestParam Long id,
                                            @RequestBody Course courseDetails) {
        CourseService.CourseResult result =
                courseService.updateCourse(id, courseDetails);
        if (result.success()) {
            return ApiResponse.ok(result.course());
        } else {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, result.message());
        }
    }
}
