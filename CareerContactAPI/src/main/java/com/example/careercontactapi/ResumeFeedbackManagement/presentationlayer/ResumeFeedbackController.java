package com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer;

import com.example.careercontactapi.ResumeFeedbackManagement.businesslayer.ResumeFeedbackService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Task 10.2: accepts a resume file and returns AI-generated feedback.
 * The path sits under /api/v1/resumes/**, which SecurityConfig already
 * restricts to authenticated sessions.
 */
@RestController
@RequestMapping("/api/v1/resumes/feedback")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ResumeFeedbackController {

    private final ResumeFeedbackService resumeFeedbackService;

    public ResumeFeedbackController(ResumeFeedbackService resumeFeedbackService) {
        this.resumeFeedbackService = resumeFeedbackService;
    }

    @PostMapping
    public ResponseEntity<ResumeFeedbackResponseModel> requestFeedback(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resumeFeedbackService.requestFeedback(principal.getSubject(), file));
    }
}
