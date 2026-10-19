package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({
        "/api/v1/account/job-postings",
        "/account/job-postings"
})
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PutMapping("/{jobId}/applications")
    public ResponseEntity<JobApplicationResponseModel> updateApplication(
            @PathVariable Integer jobId,
            @RequestBody(required = false) UpdateJobApplicationRequestModel requestModel,
            @AuthenticationPrincipal OidcUser principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(jobApplicationService.updateApplication(
                jobId,
                principal.getSubject(),
                requestModel == null ? null : requestModel.applicationId(),
                requestModel == null ? null : requestModel.status()
        ));
    }
}
