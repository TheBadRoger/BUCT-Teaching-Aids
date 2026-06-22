package com.buctta.api.controller;

import com.buctta.api.dto.BindingRequest;
import com.buctta.api.dto.BindingResponse;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserBindingService;
import com.buctta.api.service.UserBindingService.BindingResult;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户身份绑定控制器
 * 提供绑定/解绑学生或教师身份的接口
 */
@RestController
@RequestMapping("/api/user/binding")
public class UserBindingCtrl {

    @Resource
    private UserBindingService userBindingService;

    /**
     * 绑定身份接口
     * 需要已登录用户
     */
    @PostMapping("/bind")
    public ApiResponse<BindingResponse> bind(@RequestBody BindingRequest request) {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }

        // 参数校验
        if (request.getName() == null || request.getName().isEmpty()) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "name");
        }
        if (request.getIdCard() == null || request.getIdCard().isEmpty()) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "idCard");
        }
        if (request.getBindingType() == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "bindingType");
        }

        BindingResult result;

        switch (request.getBindingType()) {
            case STUDENT:
                if (request.getStudentNumber() == null || request.getStudentNumber().isEmpty()) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "studentNumber");
                }
                result = userBindingService.bindStudent(
                        currentUser.getId(),
                        request.getName(),
                        request.getIdCard(),
                        request.getStudentNumber()
                );
                break;

            case TEACHER:
                if (request.getEmployeeNumber() == null || request.getEmployeeNumber().isEmpty()) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "employeeNumber");
                }
                result = userBindingService.bindTeacher(
                        currentUser.getId(),
                        request.getName(),
                        request.getIdCard(),
                        request.getEmployeeNumber()
                );
                break;

            default:
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
        }

        if (result.success()) {
            BindingResponse response = BindingResponse.from(
                    result.user(),
                    result.student(),
                    result.teacher()
            );
            return ApiResponse.ok(response);
        }
        else {
            return handleBindingError(result);
        }
    }

    /**
     * 解绑身份接口
     * 需要已登录用户
     */
    @PostMapping("/unbind")
    public ApiResponse<Void> unbind() {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }

        BindingResult result = userBindingService.unbind(currentUser.getId());

        if (result.success()) {
            return ApiResponse.ok();
        }
        else {
            return handleBindingError(result);
        }
    }

    /**
     * 获取当前用户绑定信息
     */
    @GetMapping("/info")
    public ApiResponse<BindingResponse> getBindingInfo() {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }

        Student student = userBindingService.getBoundStudent(currentUser.getId());
        Teacher teacher = userBindingService.getBoundTeacher(currentUser.getId());

        BindingResponse response = BindingResponse.from(currentUser, student, teacher);
        return ApiResponse.ok(response);
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    /**
     * 处理绑定错误
     */
    private <T> ApiResponse<T> handleBindingError(BindingResult result) {
        return switch (result.errorCode()) {
            case "USER_NOT_FOUND" -> ApiResponse.fail(BusinessStatus.USER_NOT_FOUND);
            case "BINDIND_CONFLICT" -> ApiResponse.fail(BusinessStatus.BINDING_CONFLICT);
            case "ALREADY_BOUND" -> ApiResponse.fail(BusinessStatus.ALREADY_BOUND);
            case "STUDENT_NUMBER_BOUND", "TEACHER_BOUND" -> ApiResponse.fail(BusinessStatus.IDENTITY_ALREADY_BOUND);
            case "IDENTITY_VERIFY_FAILED" -> ApiResponse.fail(BusinessStatus.IDENTITY_VERIFY_FAILED, result.message());
            case "NOT_BOUND" -> ApiResponse.fail(BusinessStatus.NOT_BOUND);
            default -> ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        };
    }
}

