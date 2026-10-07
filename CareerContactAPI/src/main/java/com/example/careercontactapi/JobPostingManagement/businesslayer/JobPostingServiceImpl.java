package com.example.careercontactapi.JobPostingManagement.businesslayer;

import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingRequestModel;
import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobPostingServiceImpl implements JobPostingService {
    private final JobPostingRepository jobPostingRepository;

    public JobPostingServiceImpl(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    @Override
    public JobPostingResponseModel createJobPosting(JobPostingRequestModel requestModel) {
        // Should check if user exists beforehand

        JobPosting jobPosting = JobPosting.builder()
                .userId(requestModel.getUserId())
                .jobName(requestModel.getJobName())
                .company(requestModel.getCompany())
                .description(requestModel.getDescription())
                .location(requestModel.getLocation())
                .category(requestModel.getCategory())
                .salary(requestModel.getSalary())
                .dateCreated(LocalDateTime.now())
                .deadline(requestModel.getDeadline())
                .build();

        return toResponseModel(jobPostingRepository.save(jobPosting));
    }

    @Override
    public List<JobPostingResponseModel> getJobPostings() {
        return jobPostingRepository
                .findAll()
                .stream()
                .map(this::toResponseModel)
                .toList();
    }

    @Override
    public JobPostingResponseModel getJobPostingById(Integer jobId) {
        JobPosting jobPosting = jobPostingRepository
                .findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id" + jobId + " not found"
                ));

        return toResponseModel(jobPosting);
    }

    private JobPostingResponseModel toResponseModel(JobPosting jobPosting) {
        return new JobPostingResponseModel(
                jobPosting.getId(),
                jobPosting.getUserId(),
                jobPosting.getJobName(),
                jobPosting.getCompany(),
                jobPosting.getDescription(),
                jobPosting.getLocation(),
                jobPosting.getCategory(),
                jobPosting.getSalary(),
                jobPosting.getDateCreated(),
                jobPosting.getDeadline()
        );
    }
}