package com.example.careercontactapi.JobApplicationManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Integer> {

    boolean existsByJobPostingIdAndUserId(Integer jobPostingId, String userId);

    List<JobApplication> findAllByJobPostingId(Integer jobPostingId);
}