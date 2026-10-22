package com.example.careercontactapi.ResumeManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    List<Resume> findByUserIdOrderByUploadedAtDesc(String userId);
    Resume findByIdAndUserId(Integer id, String userId);
}