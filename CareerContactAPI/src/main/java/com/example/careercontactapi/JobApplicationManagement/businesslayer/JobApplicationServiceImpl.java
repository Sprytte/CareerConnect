package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationRequestModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationResponseModel;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.ResumeManagement.datalayer.Resume;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ResumeRepository resumeRepository;

    public JobApplicationServiceImpl(
            JobApplicationRepository jobApplicationRepository,
            JobPostingRepository jobPostingRepository,
            ResumeRepository resumeRepository
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.resumeRepository = resumeRepository;
    }

    @Override
    public JobApplicationResponseModel applyToJobPosting(
            Integer jobId,
            String userId,
            JobApplicationRequestModel requestModel
    ) {
        if (!jobPostingRepository.existsById(jobId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job posting with id " + jobId + " not found"
            );
        }

        Integer resumeId = requestModel == null ? null : requestModel.resumeId();
        Resume resume = resumeId == null
                ? resumeRepository.findByUserIdOrderByUploadedAtDesc(userId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A resume is required to apply"
                ))
                : resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        if (!resume.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only apply with your own resume"
            );
        }

        JobApplication application = JobApplication.builder()
                .jobPostingId(jobId)
                .userId(userId)
                .resumeId(resume.getId())
                .status("SUBMITTED")
                .appliedAt(LocalDateTime.now())
                .build();

        return toResponseModel(jobApplicationRepository.save(application));
    }

    private JobApplicationResponseModel toResponseModel(JobApplication application) {
        return new JobApplicationResponseModel(
                application.getId(),
                application.getJobPostingId(),
                application.getUserId(),
                application.getResumeId(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
