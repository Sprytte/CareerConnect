package com.example.careercontactapi.UserManagement.presentationlayer;

import lombok.*;

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
}
