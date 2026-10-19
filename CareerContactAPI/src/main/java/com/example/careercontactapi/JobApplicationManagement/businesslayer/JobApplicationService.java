package com.example.careercontactapi.JobApplicationManagement.businesslayer;

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

    JobApplicationResponseModel updateApplication(
            Integer jobId,
            String ownerId,
            Integer applicationId,
            String status
    );
}
