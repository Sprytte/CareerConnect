package com.example.careercontactapi.UserManagement.presentationlayer;


import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import com.example.careercontactapi.security.service.Auth0PasswordService;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
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
    private final Auth0PasswordService passwordService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseModel> getUser(@PathVariable String userId,
            @AuthenticationPrincipal OidcUser principal) {
        requireOwner(userId, principal);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseModel> updateUser(
            @PathVariable String userId,
            @RequestBody UserUpdateRequestModel userUpdateRequestModel,
            @AuthenticationPrincipal OidcUser principal
    ) {
        requireOwner(userId, principal);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(userService.updateUser(userId, userUpdateRequestModel));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String userId,
            @RequestBody PasswordUpdateRequestModel request,
            @AuthenticationPrincipal OidcUser principal,
            HttpServletRequest servletRequest) {
        requireOwner(userId, principal);
        passwordService.changePassword(principal.getSubject(), request.password());
        var session = servletRequest.getSession(false);
        if (session != null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void requireOwner(String userId, OidcUser principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!principal.getSubject().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own profile");
        }
    }

}
