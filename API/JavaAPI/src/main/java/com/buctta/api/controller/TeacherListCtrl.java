package com.buctta.api.controller;

import com.buctta.api.dto.TeacherDTO;
import com.buctta.api.entities.Teacher;
import com.buctta.api.service.TeacherService;
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
@RequestMapping("/api/teacher")
public class TeacherListCtrl {

    @Resource
    private TeacherService teacherService;

    @PostMapping("/add")
    public ApiResponse<Teacher> addteacherCall(@RequestBody Teacher newteacher) {
        TeacherService.TeacherResult result = teacherService.addTeacher(newteacher);
        if (result.success()) {
            return ApiResponse.ok(result.teacher());
        } else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS, result.message());
        }
    }

    //  搜索接口，新增 User 相关参数
    @PostMapping("/search")
    public ApiResponse<Page<TeacherDTO>> SearchTeacherCall(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String jointime,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        try {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<TeacherDTO> teacherPage = teacherService.searchTeachers(
                    name, organization, jointime, gender, education,
                    username, telephone, email, userType, pageable);
            return ApiResponse.ok(teacherPage);
        } catch (Exception e) {
            log.error("搜索教师时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    @DeleteMapping("/batch")
    public ApiResponse<String> deleteTeachers(@RequestBody List<Long> ids) {
        TeacherService.TeacherResult result = teacherService.deleteTeachers(ids);
        if (result.success()) {
            return ApiResponse.ok(result.message());
        } else {
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, result.message());
        }
    }

    //  导出接口，同样支持 User 字段过滤
    @GetMapping("/export")
    public void exportTeachers(HttpServletResponse response,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String organization,
                               @RequestParam(required = false) String jointime,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String education,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String telephone,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String userType)
            throws IOException {

        List<Teacher> teachers = teacherService.searchTeachersBySpec(
                name, organization, jointime, gender, education,
                username, telephone, email, userType);
        byte[] excelBytes = teacherService.exportTeachersToExcel(teachers);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=teachers.xlsx");
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
    /**
     * 编辑教师信息
     */
    @PutMapping("/update")
    public ApiResponse<Teacher> updateTeacher(@RequestParam Long id,
                                              @RequestBody Teacher teacherDetails) {
        TeacherService.TeacherResult result =
                teacherService.updateTeacher(id, teacherDetails);
        if (result.success()) {
            return ApiResponse.ok(result.teacher());
        } else {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, result.message());
        }
    }
}