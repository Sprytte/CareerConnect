package com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer;

import com.example.careercontactapi.ResumeFeedbackManagement.businesslayer.ResumeFeedbackService;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task 10.1: request AI feedback for a resume already saved to the user's
 * account. The path is mapped under both the existing resume resource and the
 * account-scoped route named in the issue.
 *
 * <p>The resume is never accepted from the client here: the id is resolved
 * against the authenticated user's saved resumes by the service.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/resumes", "/api/v1/account/resumes"})
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SavedResumeFeedbackController {

    private final ResumeFeedbackService resumeFeedbackService;
    private final UserService userService;

    @PostMapping("/{resumeId}/feedback")
    public ResponseEntity<ResumeFeedbackResponseModel> requestFeedback(
            @PathVariable Integer resumeId,
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.requireRole(principal.getSubject(), UserRole.JOB_SEEKER);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resumeFeedbackService.requestFeedbackForResume(
                        principal.getSubject(),
                        resumeId
                ));
    }
}
