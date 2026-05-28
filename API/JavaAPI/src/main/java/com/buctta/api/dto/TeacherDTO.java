package com.buctta.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {
    private Long id;
    private String name;
    private String organization;
    private String gender;
    private String education;
    private String jointime;

    // 展开的 User 字段（可为 null）
    private String username;
    private String telephone;
    private String email;
    private String userType;   // User.UserType 枚举名
}