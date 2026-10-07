package com.example.careercontactapi.JobPostingManagement.businesslayer;

import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingRequestModel;
import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingResponseModel;

import java.util.List;

public interface JobPostingService {
    JobPostingResponseModel createJobPosting(JobPostingRequestModel jobPostingRequestModel);
    List<JobPostingResponseModel> getJobPostings();
    JobPostingResponseModel getJobPostingById(Integer jobId);
}