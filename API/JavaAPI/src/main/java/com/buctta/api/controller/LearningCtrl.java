package com.buctta.api.controller;

import com.buctta.api.dto.LearningStatsDTO;
import com.buctta.api.entities.User;
import com.buctta.api.service.LearningStatsService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习统计聚合接口，同时服务「成长地图」与「学习分布」。
 * <p>
 * 身份解析优先级：显式传入的 studentId / teacherId &gt; 当前登录用户绑定的身份。
 * 学生能查任意学生（学习数据本身是公开统计口径）；如需收紧可在此加权限判断。
 */
@Slf4j
@RestController
@RequestMapping("/api/learning")
public class LearningCtrl {

    @Resource
    private LearningStatsService learningStatsService;

    /**
     * 取学习统计。
     *
     * @param studentId 学生 ID；省略时取当前登录用户绑定的学生身份
     * @param teacherId 教师 ID；省略时取当前登录用户绑定的教师身份
     * @param role      当登录用户同时能解析出两种身份时用于消歧：STUDENT / TEACHER，默认 STUDENT
     */
    @GetMapping("/stats")
    public ApiResponse<LearningStatsDTO> stats(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String role) {
        Long resolvedStudentId = studentId;
        Long resolvedTeacherId = teacherId;

        User current = currentUser();
        if (resolvedStudentId == null && resolvedTeacherId == null && current != null) {
            boolean wantTeacher = "TEACHER".equalsIgnoreCase(role)
                    || (role == null && current.getTeacher() != null && current.getStudent() == null);
            if (wantTeacher && current.getTeacher() != null) {
                resolvedTeacherId = current.getTeacher().getId();
            }
            else if (current.getStudent() != null) {
                resolvedStudentId = current.getStudent().getId();
            }
            else if (current.getTeacher() != null) {
                resolvedTeacherId = current.getTeacher().getId();
            }
        }

        try {
            LearningStatsDTO result;
            if (resolvedStudentId != null) {
                result = learningStatsService.forStudent(resolvedStudentId);
                if (result == null) {
                    return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND,
                            "学生不存在，ID: " + resolvedStudentId);
                }
            }
            else if (resolvedTeacherId != null) {
                result = learningStatsService.forTeacher(resolvedTeacherId);
                if (result == null) {
                    return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND,
                            "教师不存在，ID: " + resolvedTeacherId);
                }
            }
            else {
                return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                        "无法确定统计对象：请提供 studentId / teacherId，或以已绑定身份的用户登录");
            }
            return ApiResponse.ok(result);
        }
        catch (RuntimeException e) {
            log.error("查询学习统计失败 studentId={} teacherId={}", resolvedStudentId,
                    resolvedTeacherId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
