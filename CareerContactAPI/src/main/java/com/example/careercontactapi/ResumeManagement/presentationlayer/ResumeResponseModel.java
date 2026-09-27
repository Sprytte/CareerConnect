package com.example.careercontactapi.ResumeManagement.presentationlayer;

import java.time.LocalDateTime;

public record ResumeResponseModel(
        Integer id,
        String userId,
        String fileName,
        String contentType,
        Long sizeBytes,
        LocalDateTime uploadedAt
) {
}