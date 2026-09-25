package com.example.careercontactapi.UserManagement.businesslayer;
//import com.example.cwcp.exceptions.NotFoundException;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import com.example.careercontactapi.UserManagement.datamapperlayer.UserRequestMapper;
import com.example.careercontactapi.UserManagement.datamapperlayer.UserResponseMapper;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserRequestMapper userRequestMapper;
    private final UserResponseMapper userResponseMapper;

    @Override
    public UserResponseModel addUser(UserRequestModel userRequestModel) {
        if (userRepository.existsByUserId(userRequestModel.getUserId()))
            throw new UnsupportedOperationException("User with userId: " + userRequestModel.getUserId() + " already exists.");

        User user = userRequestMapper.requestModelToEntity(userRequestModel);
        userRepository.save(user);
        return userResponseMapper.entityToResponseModel(user);
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

}
