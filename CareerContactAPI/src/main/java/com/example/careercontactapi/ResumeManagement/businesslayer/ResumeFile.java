package com.example.careercontactapi.ResumeManagement.businesslayer;

import org.springframework.core.io.Resource;
public record ResumeFile(
        Resource resource,
        String originalName,
        String contentType
) {
}