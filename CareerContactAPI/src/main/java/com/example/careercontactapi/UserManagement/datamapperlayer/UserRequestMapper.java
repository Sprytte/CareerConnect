package com.example.careercontactapi.UserManagement.datamapperlayer;

import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserRequestMapper {
    @Mapping(target = "id", ignore = true)
    User requestModelToEntity(UserRequestModel userRequestModel);
}
