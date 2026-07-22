package com.buctta.api.controller;

import com.buctta.api.dto.StudentDTO;
import com.buctta.api.dto.StudentWithUserRequest;
import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/students")
public class StudentCtrl {

    @Resource
    private StudentService studentService;

    @PostMapping("/add")
    public ApiResponse<Student> addStudent(@RequestBody Student student) {
        StudentService.StudentResult result = studentService.addStudent(student);
        if (result.success()) {
            return ApiResponse.ok(result.student());
        } else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS, result.message());
        }
    }

    // 搜索接口，telephone 和 email 已改为查询 User 表
    @GetMapping("/search")
    public ApiResponse<Page<StudentDTO>> searchStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<StudentDTO> studentPage = studentService.searchStudents(
                    name, studentNumber, className, gender, telephone, email, pageable);
            return ApiResponse.ok(studentPage);
        } catch (Exception e) {
            log.error("搜索学生时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
    // 批量删除
    @DeleteMapping("/batch")
    public ApiResponse<String> deleteStudents(@RequestBody List<Long> ids) {
        StudentService.StudentResult result = studentService.deleteStudents(ids);
        if (result.success()) {
            return ApiResponse.ok(result.message());
        } else {
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, result.message());
        }
    }

    // 导出接口，telephone 和 email 查询 User 表
    @GetMapping("/export")
    public void exportStudents(HttpServletResponse response,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String studentNumber,
                               @RequestParam(required = false) String className,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String telephone,
                               @RequestParam(required = false) String email)
            throws IOException {

        List<Student> students = studentService.searchStudentsBySpec(
                name, studentNumber, className, gender, telephone, email);
        byte[] excelBytes = studentService.exportStudentsToExcel(students);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=students.xlsx");
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
    /**
     * 编辑学生信息
     */
    @PutMapping("/update")
    public ApiResponse<Student> updateStudent(@RequestParam Long id,
                                              @RequestBody Student studentDetails) {
        StudentService.StudentResult result =
                studentService.updateStudent(id, studentDetails);
        if (result.success()) {
            return ApiResponse.ok(result.student());
        } else {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, result.message());
        }
    }
    /**
     * 新增学生并创建绑定用户
     */
    @PostMapping("/add-with-user")
    public ApiResponse<Map<String, Object>> addStudentWithUser(
            @RequestBody StudentWithUserRequest request) {
        StudentService.AddWithUserResult result = studentService.addStudentWithUser(
                request.getStudent(),
                request.getUsername(),
                request.getPassword(),
                request.getTelephone(),
                request.getEmail()
        );

        if (result.success()) {
            Map<String, Object> data = new HashMap<>();
            data.put("student", result.student());
            data.put("user", result.user());
            return ApiResponse.ok(data);
        } else {
            return ApiResponse.fail(BusinessStatus.ENTITY_EXISTS, result.message());
        }
    }
}