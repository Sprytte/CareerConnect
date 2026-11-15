package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.JobApplicationService;
import com.example.careercontactapi.JobPostingManagement.businesslayer.JobPostingService;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account/job-postings")
public class RecruiterResumeController {

    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;
    private final ResumeService resumeService;

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
