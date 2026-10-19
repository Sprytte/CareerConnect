package com.example.careercontactapi.ResumeManagement.businesslayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.careercontactapi.ResumeManagement.presentationlayer.ResumeResponseModel;
import java.util.List;

public interface ResumeService {
    ResumeResponseModel uploadResume(String userId, MultipartFile fichier);
    ResumeResponseModel uploadResumeForUser(String requestedUserId, String authenticatedUserId, MultipartFile file);
    List<ResumeResponseModel> getResumesByUser(String userId);
    List<ResumeResponseModel> getResumesForUser(String requestedUserId, String authenticatedUserId);
    ResumeFile downloadResume(Integer resumeId);
    ResumeFile downloadResumeForUser(Integer resumeId, String authenticatedUserId);
    void deleteResume(Integer resumeId);
    void deleteResumeForUser(Integer resumeId, String authenticatedUserId);
}