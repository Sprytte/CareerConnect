package com.example.careercontactapi.UserManagement.presentationlayer;

import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseModel {
    private String userId;
    private String name;
    private String email;
    private String pictureUrl;
    private UserRole role;
    private List<String> permissions;
}
