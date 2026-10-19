package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

public record UpdateJobApplicationRequestModel(
        Integer applicationId,
        String status
) {
}
