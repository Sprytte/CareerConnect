package com.example.careercontactapi.JobApplicationManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Integer> {

    boolean existsByJobPostingIdAndUserId(Integer jobPostingId, String userId);
}
