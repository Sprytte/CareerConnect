package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import com.example.careercontactapi.JobPostingManagement.businesslayer.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account/job-postings")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AccountJobPostingController {

    private final JobPostingService jobPostingService;

    @GetMapping("")
    public ResponseEntity<List<JobPostingResponseModel>> getCreatedJobPostings(
            @AuthenticationPrincipal OidcUser principal
    ) {
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        return ResponseEntity.ok(
                jobPostingService.getJobPostingsByUserId(
                        principal.getSubject()
                )
        );
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobPostingResponseModel> getCreatedJobPostingById(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable Integer jobId
    ) {
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        return ResponseEntity.ok(
                jobPostingService.getJobPostingByIdAndUserId(
                        jobId,
                        principal.getSubject()
                )
        );
    }
}