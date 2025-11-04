package com.buctta.adminweb.entities;

import jakarta.persistence.*;

@Table(name="teacher_list")
@Entity
public class TeacherList {
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

    public String getName(){return name;}
    public String getOrganization(){return organization;}
    public String getGender(){return gender;}
    public String getEducation(){return education;}
    public String getTelephone(){return telephone;}
    public String getEmail(){return email;}
    public String getJointime(){return jointime;}
    public Long getId(){return id;}
}
