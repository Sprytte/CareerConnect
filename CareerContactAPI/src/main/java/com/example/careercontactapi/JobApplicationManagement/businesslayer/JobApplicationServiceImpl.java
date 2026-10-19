package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationRequestModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.JobApplicationResponseModel;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.ResumeManagement.datalayer.Resume;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final Map<String, String> VALID_STATUSES = Map.of(
            "INTERVIEW", "Interview",
            "OFFER", "Offer",
            "REJECT", "Reject"
    );

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
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));

        if (jobPosting.getDeadline() != null
                && !LocalDateTime.now().isBefore(jobPosting.getDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The deadline for this job posting has passed"
            );
        }

        if (jobApplicationRepository.existsByJobPostingIdAndUserId(jobId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already applied to this job posting"
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

    @Override
    public JobApplicationResponseModel updateApplication(
            Integer jobId,
            String ownerId,
            Integer applicationId,
            String status
    ) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));

        if (!jobPosting.getUserId().equals(ownerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner of this job posting can edit applications"
            );
        }

        if (applicationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An applicationId is required"
            );
        }

        String normalizedStatus = status == null
                ? null
                : VALID_STATUSES.get(status.trim().toUpperCase(Locale.ROOT));
        if (normalizedStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be one of: Interview, Offer, Reject"
            );
        }

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application with id " + applicationId + " not found"
                ));

        if (!jobId.equals(application.getJobPostingId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application with id " + applicationId
                            + " not found for job posting " + jobId
            );
        }

        application.setStatus(normalizedStatus);
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
