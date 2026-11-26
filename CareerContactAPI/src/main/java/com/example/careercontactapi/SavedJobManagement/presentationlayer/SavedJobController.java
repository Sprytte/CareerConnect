package com.example.careercontactapi.SavedJobManagement.presentationlayer;

import com.example.careercontactapi.SavedJobManagement.businesslayer.SavedJobService;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Saved jobs of the signed-in job seeker. The account always comes from the
 * session, never from the request, and recruiter accounts are refused.
 */
@RestController
@RequestMapping("/api/v1/account/saved-jobs")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SavedJobController {

    private final SavedJobService savedJobService;
    private final UserService userService;

    public SavedJobController(SavedJobService savedJobService, UserService userService) {
        this.savedJobService = savedJobService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<SavedJobResponseModel>> getSavedJobs(
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.requireRole(principal.getSubject(), UserRole.JOB_SEEKER);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(savedJobService.getSavedJobs(principal.getSubject()));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<SavedJobResponseModel> saveJob(
            @PathVariable Integer jobId,
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.requireRole(principal.getSubject(), UserRole.JOB_SEEKER);

        return ResponseEntity.ok(savedJobService.saveJob(principal.getSubject(), jobId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> removeSavedJob(
            @PathVariable Integer jobId,
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.requireRole(principal.getSubject(), UserRole.JOB_SEEKER);

        savedJobService.removeSavedJob(principal.getSubject(), jobId);
        return ResponseEntity.noContent().build();
    }
}
