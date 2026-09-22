package com.example.careercontactapi.security;

import com.example.careercontactapi.security.service.Auth0LoginService;
import com.example.careercontactapi.security.service.Auth0ManagementService;
import com.example.careercontactapi.security.usermodels.EmployeeRequestModel;
import com.example.careercontactapi.security.usermodels.EmployeeResponseModel;
import com.example.careercontactapi.security.usermodels.UserInfoResponseModel;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Slf4j
@Generated
@RestController
@RequestMapping("/api/v1/cc/security")
@CrossOrigin(origins = {"http://localhost:3000"},allowCredentials = "true")
public class SecurityController {
    private final Auth0LoginService auth0LoginService;
    private final Auth0ManagementService auth0ManagementService;
    private final UserService userService;

    @Value("${backend.url}")
    private String backendDomain;

    public SecurityController(Auth0LoginService auth0LoginService, Auth0ManagementService auth0ManagementService, UserService userService,@Value("${backend.url}") String backendDomain) {
        this.auth0LoginService = auth0LoginService;
        this.auth0ManagementService = auth0ManagementService;
        this.userService = userService;
        this.backendDomain = backendDomain;
    }

    @GetMapping("/redirect")
    public ResponseEntity<Void> redirectAfterLogin(@AuthenticationPrincipal OidcUser principal) throws IOException, InterruptedException {
        if (principal == null) {
            log.info("Principal is null");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(backendDomain + "oauth2/authorization/okta")).build();
        }
        log.info("Principal is not null, userId: {}", principal.getSubject());
        if(userService.userExists(principal.getSubject())){
            log.info("Customer already exists");
//            return ResponseEntity.status(HttpStatus.FOUND)
//                    .location(URI.create("http://localhost:3000/external")).build();
        } else {
            log.info("Customer does not exist");
            UserRequestModel userRequestModel = UserRequestModel.builder()
                    .userId(principal.getSubject())
                    .email(principal.getEmail())
                    .name(principal.getName())
                    .pictureUrl(principal.getClaim("picture"))
                    .build();
            userService.addUser(userRequestModel);
            log.info("Added the customer with userId: {}", principal.getSubject());
        }
        /*if(!userService.userExists(principal.getSubject())){
            UserRequestModel customerRequestModel = UserRequestModel.builder()
                    .userId(principal.getSubject())
                    .email(principal.getEmail())
                    .name(principal.getName())
                    .pictureUrl(principal.getClaim("picture"))
                    .build();
            userService.addUser(customerRequestModel);
            log.info("Added the customer with userId: {}", principal.getSubject());
        }*/
        //Change this for google and microsoft
        /*if (principal.getSubject().contains("apple") || principal.getSubject().contains("google-oauth2")
                || principal.getSubject().contains("facebook")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/external")).build();
        }*/
        /*if (principal.getSubject().contains("auth0")) {
            log.info("Principal claims: " + principal.getClaims().toString());
            if (!principal.getClaims().containsKey("https://careercontact/roles")
                    || principal.getClaim("https://careercontact/roles").toString().equals("[]")) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("http://localhost:3000/logout")).build();
            }
        }*/ //That shit does not exist
        log.info("Authorities: " + principal.getAuthorities().toString().replace(",", "-"));
        return auth0LoginService.getVoidResponseEntity(principal);
    }

    @GetMapping("/user-info/{userId}")
    public ResponseEntity<UserInfoResponseModel> getUserInfo(/*@AuthenticationPrincipal OidcUser principal, */@PathVariable String userId) throws IOException, InterruptedException {
//        if(principal == null)
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//
//        userId = principal.getSubject();
        log.info("GOT THE IDHAHAHAH: " + userId);

        return ResponseEntity.ok().body(auth0ManagementService.getUserInfo(userId));
    }
    @PatchMapping("/user-info/{userId}")
    public ResponseEntity<UserInfoResponseModel> updateUserInfo(/*@AuthenticationPrincipal OidcUser principal,*/@PathVariable String userId, @RequestBody UserRequestModel userRequestModel) throws IOException, InterruptedException {
//        if(principal == null)
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        log.info("Principal not null");
        return ResponseEntity.status(HttpStatus.CREATED).body(auth0ManagementService.updateUserInfo(userRequestModel, userId));
    }

    @PostMapping("/employees")
    public ResponseEntity<UserInfoResponseModel> addReviewEmployee(/*@AuthenticationPrincipal OidcUser principal,*/ @RequestBody EmployeeRequestModel employeeRequestModel) throws IOException, InterruptedException {
        log.info("Is inside the employees controller");
//        if(principal == null)
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        log.info("Principal not null");
        return ResponseEntity.status(HttpStatus.CREATED).body(auth0ManagementService.addReviewEmployee(employeeRequestModel));
    }

    @PatchMapping("/employees/{userId}")
    public ResponseEntity<UserInfoResponseModel> updateReviewEmployee(/*@AuthenticationPrincipal OidcUser principal,*/@PathVariable String userId, @RequestBody EmployeeRequestModel employeeRequestModel) throws IOException, InterruptedException {
        log.info("Is inside the employees controller");
//        if(principal == null)
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        log.info("Principal not null");
        return ResponseEntity.status(HttpStatus.CREATED).body(auth0ManagementService.updateReviewEmployee(employeeRequestModel, userId));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponseModel>> getReviewEmployees(/*@AuthenticationPrincipal OidcUser principal*/) throws IOException, InterruptedException {
        log.info("Is inside the employees controller");
//        if(principal == null)
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        log.info("Principal not null");
        return ResponseEntity.status(HttpStatus.CREATED).body(auth0ManagementService.getReviewEmployees());
    }

    @DeleteMapping("/deleteAccount/users/{userId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String userId) {
        try {
            auth0ManagementService.deleteUser(userId);
            return ResponseEntity.ok().build();
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
