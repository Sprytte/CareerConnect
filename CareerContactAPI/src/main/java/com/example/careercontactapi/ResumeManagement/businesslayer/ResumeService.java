package com.example.careercontactapi.ResumeManagement.businesslayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import com.example.careercontactapi.ResumeManagement.presentationlayer.ResumeResponseModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeResponseModel uploadResume(String userId, MultipartFile file);

    List<ResumeResponseModel> getResumesByUser(String userId);

    ResumeFile downloadResume(Integer resumeId, String userId);

    void deleteResume(Integer resumeId, String userId);
}