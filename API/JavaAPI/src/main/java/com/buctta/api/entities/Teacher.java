package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "teacher_list")
@Entity

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String organization;
    private String gender;
    private String education;
    private String jointime;

    // 反向关联到 User
    @OneToOne(mappedBy = "teacher")
    private User user;
}
