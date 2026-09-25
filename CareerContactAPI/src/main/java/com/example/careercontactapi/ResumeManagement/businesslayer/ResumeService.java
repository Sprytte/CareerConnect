package com.example.careercontactapi.ResumeManagement.businesslayer;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    String uploadUserImage(String userId, MultipartFile multipartFile);
}
