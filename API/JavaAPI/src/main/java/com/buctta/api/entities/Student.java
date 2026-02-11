package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "student_list")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_number", unique = true, nullable = false, length = 50)
    private String studentNumber;  // 学号

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    // 反向关联到 User
    @OneToOne(mappedBy = "student")
    private User user;
}