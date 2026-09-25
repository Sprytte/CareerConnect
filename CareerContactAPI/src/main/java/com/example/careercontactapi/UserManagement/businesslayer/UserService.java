package com.example.careercontactapi.UserManagement.businesslayer;

import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;

public interface UserService {
    UserResponseModel addUser(UserRequestModel userRequestModel);
    boolean userExists(String userId);
}
