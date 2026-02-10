package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "teacher_list")
@Entity

@Setter
@Getter
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String organization;
    private String gender;
    private String education;
    private String telephone;
    private String email;
    private String jointime;
}
