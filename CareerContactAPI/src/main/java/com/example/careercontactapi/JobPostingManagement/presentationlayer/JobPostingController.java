package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import com.example.careercontactapi.JobPostingManagement.businesslayer.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/job-postings")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping("")
    public ResponseEntity<?> createJobPosting(
            @RequestBody JobPostingRequestModel requestModel,
            @AuthenticationPrincipal OidcUser principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else if (!isValidRequestModel(requestModel)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Please make sure all fields are present and valid");
        }

        return ResponseEntity.status(201)
                .body(jobPostingService.createJobPosting(
                        requestModel,
                        principal.getSubject()
                ));
    }

    @GetMapping("")
    public ResponseEntity<List<JobPostingResponseModel>> getJobPostings() {
        return ResponseEntity.ok(jobPostingService.getJobPostings());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobPostingResponseModel> getJobPostingsById(
            @PathVariable Integer jobId
    ) {
        return ResponseEntity.ok(
                jobPostingService.getJobPostingById(jobId)
        );
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobPostingResponseModel> updateJobPosting(
            @PathVariable Integer jobId,
            @RequestBody JobPostingRequestModel requestModel
    ) {
        return ResponseEntity.ok(
                jobPostingService.updateJobPosting(jobId, requestModel)
        );
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJobPosting(
            @PathVariable Integer jobId
    ) {
        jobPostingService.deleteJobPosting(jobId);

        return ResponseEntity.noContent().build();
    }

    private boolean isValidRequestModel(JobPostingRequestModel requestModel) {
        return (requestModel.getJobName() != null && !requestModel.getJobName().isEmpty()) &&
                (requestModel.getCompany() != null && !requestModel.getCompany().isEmpty()) &&
                (requestModel.getDescription() != null && !requestModel.getDescription().isEmpty()) &&
                (requestModel.getLocation() != null && !requestModel.getLocation().isEmpty()) &&
                (requestModel.getCategory() != null && !requestModel.getCategory().isEmpty()) &&
                (requestModel.getSalary() != null && !requestModel.getSalary().isEmpty()) &&
                (requestModel.getDeadline() != null &&
                        LocalDateTime.now().isBefore(requestModel.getDeadline()));
    }
}