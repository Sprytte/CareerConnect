package com.example.careercontactapi.JobPostingManagement.businesslayer;

import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingRequestModel;
import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingResponseModel;

import java.util.List;

public interface JobPostingService {

    JobPostingResponseModel createJobPosting(
            JobPostingRequestModel jobPostingRequestModel,
            String userId
    );

    List<JobPostingResponseModel> getJobPostings();

    JobPostingResponseModel getJobPostingById(Integer jobId);

    JobPostingResponseModel updateJobPosting(
        Integer jobId,
        JobPostingRequestModel jobPostingRequestModel,
        String userId
);

void deleteJobPosting(Integer jobId, String userId);

    List<JobPostingResponseModel> getJobPostingsByUserId(String userId);

    JobPostingResponseModel getJobPostingByIdAndUserId(
            Integer jobId,
            String userId
    );
}