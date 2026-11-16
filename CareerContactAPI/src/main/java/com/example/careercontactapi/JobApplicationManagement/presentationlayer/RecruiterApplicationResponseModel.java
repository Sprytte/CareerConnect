package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import java.time.LocalDateTime;

public record RecruiterApplicationResponseModel(
        Integer id,
        Integer jobPostingId,
        String userId,
        String applicantName,
        Integer resumeId,
        String status,
        LocalDateTime appliedAt,
        LocalDateTime interviewTimeSlot
) {
}
