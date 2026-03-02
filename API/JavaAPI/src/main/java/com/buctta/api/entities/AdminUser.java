package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "admin_users")
@Entity

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {
    // 注意属性名要与数据表中的字段名一致
    // 主键自增int(10)对应long
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // 用户名属性varchar对应String
    private String username;

    // 密码属性varchar对应String
    private String password;

}