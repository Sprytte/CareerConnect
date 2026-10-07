package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import com.example.careercontactapi.JobPostingManagement.businesslayer.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/job-postings")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping("")
    public ResponseEntity<JobPostingResponseModel> createJobPosting(@RequestBody JobPostingRequestModel requestModel) {
        return ResponseEntity.status(201).body(jobPostingService.createJobPosting(requestModel));
    }

    @GetMapping("")
    public ResponseEntity<List<JobPostingResponseModel>> getJobPostings() {
        return ResponseEntity.ok(jobPostingService.getJobPostings());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobPostingsById(@PathVariable Integer jobId) {
        return ResponseEntity.ok(jobPostingService.getJobPostingById(jobId));
    }

}