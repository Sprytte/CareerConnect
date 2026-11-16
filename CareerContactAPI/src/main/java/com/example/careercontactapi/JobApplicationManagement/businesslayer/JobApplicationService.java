package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationRequestModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationResponseModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.UpdateJobApplicationRequestModel;

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
            Integer jobId,
            String ownerId,
            UpdateJobApplicationRequestModel requestModel
    );
}
