package com.example.careercontactapi.ResumeManagement.businesslayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import com.example.careercontactapi.ResumeManagement.presentationlayer.ResumeResponseModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {
    ResumeResponseModel uploadResumeForUser(String authenticatedUserId, MultipartFile file);
    List<ResumeResponseModel> getResumesForUser(String authenticatedUserId);
    ResumeFile downloadResumeForUser(Integer resumeId, String authenticatedUserId);
    ResumeResponseModel updateResume(
            Integer resumeId,
            String newFilename,
            String authenticatedUserId
    );
    void deleteResumeForUser(Integer resumeId, String authenticatedUserId);
}
