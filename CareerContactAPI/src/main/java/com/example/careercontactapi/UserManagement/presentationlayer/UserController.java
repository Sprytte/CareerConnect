package com.example.careercontactapi.UserManagement.presentationlayer;


import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseModel> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseModel> updateUser(
            @PathVariable String userId,
            @RequestBody UserUpdateRequestModel userUpdateRequestModel
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, userUpdateRequestModel));
    }

}
