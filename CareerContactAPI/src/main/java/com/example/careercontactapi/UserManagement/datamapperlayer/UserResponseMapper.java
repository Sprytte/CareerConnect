package com.example.careercontactapi.UserManagement.datamapperlayer;

import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {
    @Mapping(
            target = "permissions",
            expression = "java(user.getRole() == null ? java.util.List.of() : user.getRole().getPermissions())"
    )
    UserResponseModel entityToResponseModel(User user);
}
