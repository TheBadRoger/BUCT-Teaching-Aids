package com.buctta.api.dto;

import com.buctta.api.entities.Student;
import lombok.Data;

@Data
public class StudentWithUserRequest {
    private Student student;
    private String username;
    private String password;
    private String telephone;
    private String email;
}