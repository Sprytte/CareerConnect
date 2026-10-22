package com.example.careercontactapi.ResumeManagement.businesslayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.careercontactapi.ResumeManagement.presentationlayer.ResumeResponseModel;
import java.util.List;

public interface ResumeService {
    ResumeResponseModel uploadResume(String userId, MultipartFile fichier);
    List<ResumeResponseModel> getResumesByUser(String userId);
    ResumeFile downloadResume(Integer resumeId);
    ResumeResponseModel updateResume(Integer resumeId, String newFilename, String userId);
    void deleteResume(Integer resumeId, String userId);
}