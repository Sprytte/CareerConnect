package com.example.careercontactapi.UserManagement.businesslayer;
//import com.example.cwcp.exceptions.NotFoundException;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import com.example.careercontactapi.UserManagement.datalayer.UserConstraintViolations;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
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
import org.springframework.dao.DataAccessException;

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
        } catch (DataAccessException e) {
            if (!UserConstraintViolations.isUniqueViolation(e)) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email address is already in use");
        }
    }

    @Override
    public UserResponseModel setRole(String userId, UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose whether you are a job seeker or a recruiter"
            );
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with userId: " + userId + " not found"
                ));

        // The role is chosen once, immediately after account creation. Changing
        // it later would orphan postings or applications that belong to the old
        // account type, so an explicit support process is safer than a silent swap.
        if (user.getRole() != null) {
            if (user.getRole() == role) {
                return userResponseMapper.entityToResponseModel(user);
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This account type has already been set"
            );
        }

        user.setRole(role);
        return userResponseMapper.entityToResponseModel(userRepository.saveAndFlush(user));
    }

    @Override
    public void requireRole(String userId, UserRole role) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sign in again to continue"
                ));

        if (user.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Choose your account type before using this feature"
            );
        }

        if (user.getRole() != role) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This action requires a " + role.getDisplayName() + " account"
            );
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
