package com.buctta.api.controller;

import com.buctta.api.service.ProfileService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生资料（只读）。
 * <p>
 * 一次返回「基础资料 + 学习统计」，供「学生-个人资料」页使用，
 * 避免前端为渲染一页分别请求资料与统计两个接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileCtrl {

    @Resource
    private ProfileService profileService;

    /** 学生资料 + 学习统计 */
    @GetMapping("/{studentId}")
    public ApiResponse<ProfileService.StudentProfileView> byStudentId(@PathVariable Long studentId) {
        try {
            ProfileService.StudentProfileView view = profileService.studentProfile(studentId);
            if (view == null) {
                return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND,
                        "学生不存在，ID: " + studentId);
            }
            return ApiResponse.ok(view);
        }
        catch (RuntimeException e) {
            log.error("读取学生资料失败 studentId={}", studentId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}
