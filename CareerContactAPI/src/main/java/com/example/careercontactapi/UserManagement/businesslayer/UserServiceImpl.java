package com.example.careercontactapi.UserManagement.businesslayer;
//import com.example.cwcp.exceptions.NotFoundException;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import com.example.careercontactapi.UserManagement.datamapperlayer.UserRequestMapper;
import com.example.careercontactapi.UserManagement.datamapperlayer.UserResponseMapper;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserUpdateRequestModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

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
    public UserResponseModel getUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with userId: " + userId + " not found"
                ));

        return userResponseMapper.entityToResponseModel(user);
    }

    @Override
    public UserResponseModel updateUser(String userId, UserUpdateRequestModel userUpdateRequestModel) {
        if (userUpdateRequestModel == null
                || isBlank(userUpdateRequestModel.getName())
                || isBlank(userUpdateRequestModel.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name and email are required"
            );
        }

        if (!userUpdateRequestModel.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        if (userUpdateRequestModel.getPassword() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the separate password endpoint");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with userId: " + userId + " not found"
                ));

        user.setName(userUpdateRequestModel.getName().trim());
        user.setEmail(userUpdateRequestModel.getEmail().trim());

        try {
            return userResponseMapper.entityToResponseModel(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email address is already in use");
        }
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
