package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.AccountApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account/applications")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AccountApplicationController {
    private final AccountApplicationService service;

    @GetMapping
    public ResponseEntity<List<AccountApplicationResponseModel>> getApplications(
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity
                .ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.getApplications(principal.getSubject()));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<AccountApplicationResponseModel> updateApplication(
            @PathVariable Integer applicationId,
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody AccountApplicationRequestModel requestModel) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity
                .ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.updateApplication(principal.getSubject(), applicationId, requestModel));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Integer applicationId,
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteApplication(principal.getSubject(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
