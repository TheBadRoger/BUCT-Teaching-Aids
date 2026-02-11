package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_list")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;  // 用户名

    @Column(name = "telephone", length = 20)
    private String telephone;  // 电话

    @Column(name = "email", length = 100)
    private String email;  // 邮箱

    @Column(name = "password", nullable = false)
    private String password;  // 密码

    // 用户类型: TEACHER 或 STUDENT（绑定身份后自动设置）
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20)
    private UserType userType;

    // 关联 Teacher（可选，当 userType 为 TEACHER 时有值）
    @OneToOne
    @JoinColumn(name = "teacher_id", unique = true)
    private Teacher teacher;

    // 关联 Student（可选，当 userType 为 STUDENT 时有值）
    @OneToOne
    @JoinColumn(name = "student_id", unique = true)
    private Student student;

    // 用户类型枚举
    public enum UserType {
        TEACHER,
        STUDENT
    }
}
