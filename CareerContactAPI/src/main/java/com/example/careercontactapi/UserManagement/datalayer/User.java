package com.example.careercontactapi.UserManagement.datalayer;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    //TODO add more fields as necessary
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "user_id")
    private String userId;
    private String name;
    private String email;
    @Nullable
    @Column(name = "profile_picture_url")
    private String pictureUrl;
}

