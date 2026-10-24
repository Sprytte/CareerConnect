package com.example.careercontactapi.JobApplicationManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Integer> {

    boolean existsByJobPostingIdAndUserId(Integer jobPostingId, String userId);

    List<JobApplication> findAllByJobPostingId(Integer jobPostingId);

    List<JobApplication> findAllByUserIdOrderByAppliedAtDescIdDesc(String userId);

    Optional<JobApplication> findByIdAndJobPostingId(
            Integer id,
            Integer jobPostingId
    );
}