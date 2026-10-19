package com.example.careercontactapi.UserManagement.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestModel {
    private String name;
    private String email;
    private String password;
}
