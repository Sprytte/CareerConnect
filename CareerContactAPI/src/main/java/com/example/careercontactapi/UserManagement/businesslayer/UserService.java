package com.example.careercontactapi.UserManagement.businesslayer;

import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserUpdateRequestModel;

public interface UserService {
    UserResponseModel addUser(UserRequestModel userRequestModel);
    UserResponseModel getUser(String userId);
    UserResponseModel updateUser(String userId, UserUpdateRequestModel userUpdateRequestModel);
    boolean userExists(String userId);
}
