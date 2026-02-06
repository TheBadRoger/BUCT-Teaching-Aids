package com.buctta.api.controller;

import com.buctta.api.entities.TeacherList;
import com.buctta.api.service.TeacherService;
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
@RequestMapping("/api/findteacher")
public class TeacherListCtrl {
    @Resource
    private TeacherService teacherService;

    @PostMapping("/addteacher")
    public ApiResponse<TeacherList> addteacherCall(@RequestBody TeacherList newteacher) {
        TeacherList tl = teacherService.AddTeacher(newteacher);
        if (tl != null)
            return ApiResponse.ok(tl);
        else
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
    }

    @PostMapping("/searchteacher")
    public ApiResponse<Page<TeacherList>> SearchTeacherCall(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String jointime,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String education,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        try {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            // 调用服务层进行搜索
            Page<TeacherList> teacherPage = teacherService.searchTeachers(name, organization, jointime, gender, education, pageable);
            return ApiResponse.ok(teacherPage);

        }
        catch (Exception e) {
            log.error("搜索教师时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}