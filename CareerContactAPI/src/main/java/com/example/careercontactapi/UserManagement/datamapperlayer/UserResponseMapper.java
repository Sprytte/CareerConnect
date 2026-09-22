package com.example.careercontactapi.UserManagement.datamapperlayer;

import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {
    UserResponseModel entityToResponseModel(User user);
}
