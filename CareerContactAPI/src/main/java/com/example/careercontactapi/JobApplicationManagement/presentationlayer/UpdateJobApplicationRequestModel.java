package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import java.time.LocalDateTime;

public record UpdateJobApplicationRequestModel(
        Integer applicationId,
        String status,
        LocalDateTime interviewTimeSlot
) {
}
