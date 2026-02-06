package com.buctta.api.controller;

import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentCtrl {

    @Resource
    private StudentService studentService;

    @PostMapping
    public ApiResponse<Student> addStudent(@RequestBody Student student) {
        Student savedStudent = studentService.addStudent(student);
        if (savedStudent != null)
            return ApiResponse.ok(savedStudent);
        else
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);

    }

    @GetMapping("/search")
    public ApiResponse<Page<Student>> searchStudents(
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
            Page<Student> studentPage = studentService.searchStudents(name, studentNumber, className, gender, telephone, email, pageable);
            return ApiResponse.ok(studentPage);

        }
        catch (Exception e) {
            log.error("搜索学生时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}