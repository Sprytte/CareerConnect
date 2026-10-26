package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.AccountApplicationResponseModel;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.AccountApplicationRequestModel;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.ResumeManagement.datalayer.Resume;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountApplicationService {
    private final JobApplicationRepository applications;
    private final JobPostingRepository postings;
    private final ResumeRepository resumes;

    public AccountApplicationService(
            JobApplicationRepository applications,
            JobPostingRepository postings
    ) {
        this(applications, postings, null);
    }

    @Autowired
    public AccountApplicationService(
            JobApplicationRepository applications,
            JobPostingRepository postings,
            ResumeRepository resumes
    ) {
        this.applications = applications;
        this.postings = postings;
        this.resumes = resumes;
    }

    @Transactional(readOnly = true)
    public List<AccountApplicationResponseModel> getApplications(String authenticatedUserId) {
        requireAuthenticated(authenticatedUserId);

        var ownApplications = applications.findAllByUserIdOrderByAppliedAtDescIdDesc(authenticatedUserId);

        Map<Integer, JobPosting> jobs = postings.findAllById(ownApplications.stream()
                        .map(application -> application.getJobPostingId()).distinct().toList())
                .stream().collect(Collectors.toMap(JobPosting::getId, Function.identity()));

        return ownApplications.stream().map(application -> {
            var job = jobs.get(application.getJobPostingId());

            return new AccountApplicationResponseModel(application.getId(),
                    job == null ? "Job posting no longer available" : nonblank(job.getJobName(), "Job title unavailable"),
                    job == null ? "Employer unavailable" : nonblank(job.getCompany(), "Employer unavailable"),
                    displayStatus(application.getStatus()),
                    application.getAppliedAt() == null ? null : application.getAppliedAt().toLocalDate());
        }).toList();
    }

    @Transactional
    public AccountApplicationResponseModel updateApplication(
            String authenticatedUserId,
            Integer applicationId,
            AccountApplicationRequestModel requestModel
    ) {
        JobApplication application = findOwnedApplication(authenticatedUserId, applicationId);
        JobPosting posting = findEditablePosting(application);

        if (requestModel == null || requestModel.resumeId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A resumeId is required"
            );
        }

        if (resumes == null) {
            throw new IllegalStateException("ResumeRepository is required to update an application");
        }

        Resume resume = resumes.findById(requestModel.resumeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));
        if (!authenticatedUserId.equals(resume.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only use your own resume"
            );
        }

        application.setResumeId(resume.getId());
        applications.save(application);
        return toResponse(application, posting);
    }

    @Transactional
    public void deleteApplication(String authenticatedUserId, Integer applicationId) {
        JobApplication application = findOwnedApplication(authenticatedUserId, applicationId);
        findEditablePosting(application);
        applications.delete(application);
    }

    private JobApplication findOwnedApplication(String authenticatedUserId, Integer applicationId) {
        requireAuthenticated(authenticatedUserId);
        if (applicationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An applicationId is required");
        }

        JobApplication application = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));
        if (!authenticatedUserId.equals(application.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only modify your own applications"
            );
        }
        return application;
    }

    private JobPosting findEditablePosting(JobApplication application) {
        JobPosting posting = postings.findById(application.getJobPostingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting not found"
                ));
        if (posting.getDeadline() != null && !java.time.LocalDateTime.now().isBefore(posting.getDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The deadline for this job posting has passed"
            );
        }
        return posting;
    }

    private void requireAuthenticated(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private AccountApplicationResponseModel toResponse(JobApplication application, JobPosting posting) {
        return new AccountApplicationResponseModel(
                application.getId(),
                nonblank(posting.getJobName(), "Job title unavailable"),
                nonblank(posting.getCompany(), "Employer unavailable"),
                displayStatus(application.getStatus()),
                application.getAppliedAt() == null ? null : application.getAppliedAt().toLocalDate()
        );
    }

    private static String nonblank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    static String displayStatus(String value) {
        if (value == null) return null;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SUBMITTED", "APPLIED" -> "Applied";
            case "INTERVIEW" -> "Interview";
            case "OFFER", "OFFERED" -> "Offered";
            case "REJECT", "REJECTED" -> "Rejected";
            default -> value;
        };
    }
}
