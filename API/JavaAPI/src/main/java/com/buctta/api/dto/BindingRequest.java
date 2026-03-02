package com.buctta.api.dto;

import lombok.Data;

/**
 * 用户身份绑定请求DTO
 */
@Data
public class BindingRequest {

    /**
     * 绑定类型: STUDENT, TEACHER
     */
    private BindingType bindingType;

    /**
     * 姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 学号（学生绑定时使用）
     */
    private String studentNumber;

    /**
     * 工号（教师绑定时使用）
     */
    private String employeeNumber;

    public enum BindingType {
        STUDENT,   // 绑定学生身份
        TEACHER    // 绑定教师身份
    }
}

