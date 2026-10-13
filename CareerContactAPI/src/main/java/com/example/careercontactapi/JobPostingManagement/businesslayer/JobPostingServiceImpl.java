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
    public JobPostingResponseModel createJobPosting(JobPostingRequestModel requestModel, String userId) {
        // TODO Should check if user exists beforehand

        JobPosting jobPosting = JobPosting.builder()
                .userId(userId)
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
                        "Job posting with id " + jobId + " not found"
                ));

        return toResponseModel(jobPosting);
    }

    @Override
    public JobPostingResponseModel updateJobPosting(
            Integer jobId,
            JobPostingRequestModel requestModel
    ) {
        JobPosting jobPosting = jobPostingRepository
                .findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));

        jobPosting.setJobName(requestModel.getJobName());
        jobPosting.setCompany(requestModel.getCompany());
        jobPosting.setDescription(requestModel.getDescription());
        jobPosting.setLocation(requestModel.getLocation());
        jobPosting.setCategory(requestModel.getCategory());
        jobPosting.setSalary(requestModel.getSalary());
        jobPosting.setDeadline(requestModel.getDeadline());

        return toResponseModel(jobPostingRepository.save(jobPosting));
    }

    @Override
    public void deleteJobPosting(Integer jobId) {
        JobPosting jobPosting = jobPostingRepository
                .findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));

        jobPostingRepository.delete(jobPosting);
    }

    @Override
    public List<JobPostingResponseModel> getJobPostingsByUserId(String userId) {
        return jobPostingRepository
                .findAllByUserIdOrderByDeadlineDesc(userId)
                .stream()
                .map(this::toResponseModel)
                .toList();
    }

    @Override
    public JobPostingResponseModel getJobPostingByIdAndUserId(
            Integer jobId,
            String userId
    ) {
        JobPosting jobPosting = jobPostingRepository
                .findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job posting with id " + jobId + " not found"
                ));

        if (!jobPosting.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job posting with id " + jobId + " not found for this user"
            );
        }

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