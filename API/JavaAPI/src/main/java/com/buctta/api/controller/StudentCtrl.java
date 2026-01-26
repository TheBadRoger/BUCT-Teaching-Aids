package com.buctta.api.controller;

import com.buctta.api.entities.Student;
import com.buctta.api.service.StudentService;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentCtrl {

    @Resource
    private StudentService studentService;

    @PostMapping
    public ResponseContainer<Student> addStudent(@RequestBody Student student) {
        Student savedStudent = studentService.addStudent(student);
        if (savedStudent != null)
            return new ResponseContainer<>(0,"Success",savedStudent);
        else
            return new ResponseContainer<>(1001,"Success",null);

    }

    @GetMapping("/search")
    public ResponseContainer<Page<Student>> searchStudents(
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
            Page<Student> studentPage= studentService.searchStudents(name, studentNumber, className, gender, telephone,email, pageable);
            return new ResponseContainer<>(0,"搜索成功",studentPage);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseContainer<>(1004,"搜索失败",null);
        }
    }
}