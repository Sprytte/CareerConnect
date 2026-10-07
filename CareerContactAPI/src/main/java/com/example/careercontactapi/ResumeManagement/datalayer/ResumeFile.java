package com.example.careercontactapi.ResumeManagement.datalayer;

import org.springframework.core.io.Resource;
public record ResumeFile(
        Resource resource,
        String originalName,
        String contentType
) {
}