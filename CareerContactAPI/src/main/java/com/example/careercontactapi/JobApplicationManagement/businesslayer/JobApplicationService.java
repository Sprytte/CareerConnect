package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.datalayer.ApplicationStatus;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationRequestModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationResponseModel;

import java.util.List;

public interface JobApplicationService {

    JobApplicationResponseModel applyToJobPosting(
            Integer jobId,
            String userId,
            JobApplicationRequestModel requestModel
    );

    List<JobApplicationResponseModel> getApplicationsByJobPostingId(
            Integer jobId
    );

    JobApplicationResponseModel updateApplicationStatus(
            Integer applicationId,
            Integer jobId,
            ApplicationStatus status
    );
}