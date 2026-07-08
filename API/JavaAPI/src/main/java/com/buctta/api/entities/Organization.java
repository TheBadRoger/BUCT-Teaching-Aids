package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_list")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String logo;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String info;

    @Column(name = "honor_cert_url", length = 500)
    private String honorCertUrl;

    @Column(name = "created_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;

    @Column(name = "updated_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedTime;

    public Organization(String name, String logo, String bannerUrl,
                        String info, String honorCertUrl) {
        this.name = name;
        this.logo = logo;
        this.bannerUrl = bannerUrl;
        this.info = info;
        this.honorCertUrl = honorCertUrl;
    }
}