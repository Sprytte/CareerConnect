
package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.JobApplicationService;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.RecruiterApplicationResponseModel;
import com.example.careercontactapi.JobPostingManagement.businesslayer.JobPostingService;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationResponseModel;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
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
    private final JobApplicationService jobApplicationService;
    private final UserService userService;
    private final ResumeService resumeService;

    @GetMapping("")
    public ResponseEntity<List<JobPostingResponseModel>> getCreatedJobPostings(
            @AuthenticationPrincipal OidcUser principal
    ) {
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        userService.requireRole(principal.getSubject(), UserRole.RECRUITER);

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

        userService.requireRole(principal.getSubject(), UserRole.RECRUITER);

        return ResponseEntity.ok(
                jobPostingService.getJobPostingByIdAndUserId(
                        jobId,
                        principal.getSubject()
                )
        );
    }

    @PatchMapping("/{jobId}/archive")
    public ResponseEntity<JobPostingResponseModel> archiveJobPosting(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable Integer jobId
    ) {
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        userService.requireRole(principal.getSubject(), UserRole.RECRUITER);

        return ResponseEntity.ok(
                jobPostingService.archiveJobPosting(
                        jobId,
                        principal.getSubject()
                )
        );
    }

    @GetMapping("/{jobId}/applications")
    public ResponseEntity<List<RecruiterApplicationResponseModel>> getApplicationsForJobPosting(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable Integer jobId
    ) {
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        userService.requireRole(principal.getSubject(), UserRole.RECRUITER);

        jobPostingService.getJobPostingByIdAndUserId(
                jobId,
                principal.getSubject()
        );

        List<RecruiterApplicationResponseModel> applications =
                jobApplicationService.getApplicationsByJobPostingId(jobId)
                        .stream()
                        .map(application -> {
                            String applicantName = userService
                                    .getUser(application.userId())
                                    .getName();

                            return new RecruiterApplicationResponseModel(
                                    application.id(),
                                    application.jobPostingId(),
                                    application.userId(),
                                    applicantName,
                                    application.resumeId(),
                                    application.status(),
                                    application.appliedAt(),
                                    application.interviewTimeSlot()
                            );
                        })
                        .toList();

        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{jobId}/applications/{applicationId}/resume")
    public ResponseEntity<Resource> viewApplicantResume(
            @PathVariable Integer jobId,
            @PathVariable Integer applicationId,
            @AuthenticationPrincipal OidcUser principal
    ) {
        if (principal == null || principal.getSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        userService.requireRole(principal.getSubject(), UserRole.RECRUITER);

        // Only the owner of the posting can review its applications.
        jobPostingService.getJobPostingByIdAndUserId(
                jobId,
                principal.getSubject()
        );

        // Find the application within this specific posting.
        JobApplicationResponseModel application =
                jobApplicationService.getApplicationsByJobPostingId(jobId)
                        .stream()
                        .filter(item -> item.id().equals(applicationId))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Application not found"
                        ));

        // The applicant's identity comes from the verified application.
        ResumeFile resume = resumeService.downloadResumeForApplicant(
                application.resumeId(),
                application.userId()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType(resume.contentType())
        );
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename(resume.originalName(), StandardCharsets.UTF_8)
                        .build()
        );
        headers.setCacheControl("no-store");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resume.resource());
    }
}
