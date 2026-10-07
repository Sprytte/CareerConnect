package com.example.careercontactapi.JobPostingManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {
    List<JobPosting> findAllByUserIdOrderByDeadlineDesc(String userId);
}