package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "student_list")
@Getter
@Setter
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


    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    public Student() {
    }

    public Student(String studentNumber, String name, String className,
                   String gender, String telephone, String email, LocalDate admissionDate) {
        this.studentNumber = studentNumber;
        this.name = name;
        this.className = className;
        this.gender = gender;
        this.telephone = telephone;
        this.email = email;
        this.admissionDate = admissionDate;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNumber='" + studentNumber + '\'' +
                ", name='" + name + '\'' +
                ", className='" + className + '\'' +
                ", gender='" + gender + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", admissionDate=" + admissionDate +
                '}';
    }
}