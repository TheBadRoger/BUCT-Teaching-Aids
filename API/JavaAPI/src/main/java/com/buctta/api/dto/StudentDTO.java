package com.buctta.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String studentNumber;
    private String name;
    private String className;
    private String gender;
    private LocalDate admissionDate;

    // 展开的 User 字段（可为 null）
    private String username;
    private String telephone;
    private String email;
    private String userType;
}