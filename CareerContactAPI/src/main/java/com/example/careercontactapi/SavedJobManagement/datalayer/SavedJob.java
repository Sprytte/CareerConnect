package com.example.careercontactapi.SavedJobManagement.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_posting_id", nullable = false)
    private Integer jobPostingId;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;
}
