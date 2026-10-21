package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import java.time.LocalDateTime;

public record JobApplicationResponseModel(
        Integer id,
        Integer jobPostingId,
        String userId,
        Integer resumeId,
        String status,
        LocalDateTime appliedAt
) {
}
