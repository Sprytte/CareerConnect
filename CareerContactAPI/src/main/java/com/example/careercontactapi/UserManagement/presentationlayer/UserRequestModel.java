package com.example.careercontactapi.UserManagement.presentationlayer;

//import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserRequestModel {
    private String userId;
    private String name;
//    @Email
    private String email;
    private String pictureUrl;
}
