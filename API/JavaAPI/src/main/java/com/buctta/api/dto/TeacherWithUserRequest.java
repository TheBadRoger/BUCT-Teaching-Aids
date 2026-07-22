package com.buctta.api.dto;

import com.buctta.api.entities.Teacher;
import lombok.Data;

@Data
public class TeacherWithUserRequest {
    private Teacher teacher;
    private String username;
    private String password;
    private String telephone;
    private String email;
}