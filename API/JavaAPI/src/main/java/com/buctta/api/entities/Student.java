package com.buctta.api.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_list")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

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