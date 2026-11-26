package com.example.careercontactapi.SavedJobManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Integer> {

    List<SavedJob> findAllByUserIdOrderBySavedAtDescIdDesc(String userId);

    Optional<SavedJob> findByUserIdAndJobPostingId(String userId, Integer jobPostingId);
}
