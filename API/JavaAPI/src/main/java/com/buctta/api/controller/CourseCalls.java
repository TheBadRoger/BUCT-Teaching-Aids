package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.entities.User;
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
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * 发布 / 取消发布课程（**无需审批**）。
     * <p>
     * 教师可直接发布自己授课的课程。归属校验为弱校验——课程的授课教师存储为
     * 逗号分隔的姓名字符串，数据库层面没有教师外键，因此按姓名匹配；
     * 管理员登录态（principal 为 AdminUser）跳过归属校验。
     *
     * @param id        课程 ID
     * @param published 目标状态，省略时按"发布"处理
     */
    @PutMapping("/{id}/publish")
    public ApiResponse<Course> setPublished(@PathVariable Long id,
                                            @RequestParam(defaultValue = "true") Boolean published) {
        String teacherName = currentTeacherName();
        CourseService.CourseResult result = courseService.setPublished(id, published, teacherName);
        if (result.success()) {
            return ApiResponse.ok(result.course());
        }
        return switch (result.errorCode() == null ? "" : result.errorCode()) {
            case "NO_PERMISSION" -> ApiResponse.fail(BusinessStatus.NO_PERMISSION, result.message());
            case "COURSE_NOT_FOUND" -> ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, result.message());
            case "PARAM_MISSING" -> ApiResponse.fail(BusinessStatus.PARAM_MISSING, result.message());
            default -> ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, result.message());
        };
    }

    /**
     * 当前调用者的教师姓名，用于课程归属校验。
     * <p>
     * 管理员（principal 为 AdminUser）返回 null，表示跳过归属校验；
     * 未绑定教师身份的普通用户返回空串，会在服务层被拦截。
     */
    private String currentTeacherName() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }
        if (authentication.getPrincipal() instanceof com.buctta.api.entities.AdminUser) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User user && user.getTeacher() != null) {
            return user.getTeacher().getName();
        }
        return "";
    }
}
