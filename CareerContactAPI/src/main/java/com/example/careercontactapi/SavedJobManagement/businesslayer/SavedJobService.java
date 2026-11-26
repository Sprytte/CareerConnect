package com.example.careercontactapi.SavedJobManagement.businesslayer;

import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingResponseModel;
import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJob;
import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJobRepository;
import com.example.careercontactapi.SavedJobManagement.presentationlayer.SavedJobResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saved (favourite) jobs of a job seeker. Every operation is scoped to the
 * authenticated user; there is no caller-selected account.
 */
@Service
public class SavedJobService {

    private final SavedJobRepository savedJobs;
    private final JobPostingRepository postings;

    public SavedJobService(SavedJobRepository savedJobs, JobPostingRepository postings) {
        this.savedJobs = savedJobs;
        this.postings = postings;
    }

    @Transactional(readOnly = true)
    public List<SavedJobResponseModel> getSavedJobs(String authenticatedUserId) {
        requireAuthenticated(authenticatedUserId);

        List<SavedJob> saved = savedJobs.findAllByUserIdOrderBySavedAtDescIdDesc(authenticatedUserId);

        // Posting details are read in one batch rather than once per saved job.
        Map<Integer, JobPosting> jobs = postings
                .findAllById(saved.stream().map(SavedJob::getJobPostingId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));

        return saved.stream()
                .map(entry -> toResponse(entry, jobs.get(entry.getJobPostingId())))
                .toList();
    }

    /** Saving is idempotent: saving a job twice keeps the original entry. */
    @Transactional
    public SavedJobResponseModel saveJob(String authenticatedUserId, Integer jobId) {
        requireAuthenticated(authenticatedUserId);
        requireJobId(jobId);

        JobPosting posting = postings.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));
        if (posting.isArchived()) {
            // Same answer as applying to an archived posting.
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This job posting is archived and can no longer be saved"
            );
        }

        SavedJob saved = savedJobs.findByUserIdAndJobPostingId(authenticatedUserId, jobId)
                .orElseGet(() -> savedJobs.save(SavedJob.builder()
                        .userId(authenticatedUserId)
                        .jobPostingId(jobId)
                        .savedAt(LocalDateTime.now())
                        .build()));

        return toResponse(saved, posting);
    }

    /** Removing is idempotent: removing a job that is not saved succeeds. */
    @Transactional
    public void removeSavedJob(String authenticatedUserId, Integer jobId) {
        requireAuthenticated(authenticatedUserId);
        requireJobId(jobId);

        savedJobs.findByUserIdAndJobPostingId(authenticatedUserId, jobId)
                .ifPresent(savedJobs::delete);
    }

    private void requireAuthenticated(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void requireJobId(Integer jobId) {
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A jobId is required");
        }
    }

    private SavedJobResponseModel toResponse(SavedJob saved, JobPosting posting) {
        return new SavedJobResponseModel(
                saved.getJobPostingId(),
                saved.getSavedAt(),
                posting == null ? null : JobPostingResponseModel.builder()
                        .id(posting.getId())
                        .userId(posting.getUserId())
                        .jobName(posting.getJobName())
                        .company(posting.getCompany())
                        .description(posting.getDescription())
                        .location(posting.getLocation())
                        .category(posting.getCategory())
                        .salary(posting.getSalary())
                        .dateCreated(posting.getDateCreated())
                        .deadline(posting.getDeadline())
                        .archived(posting.isArchived())
                        .build()
        );
    }
}
