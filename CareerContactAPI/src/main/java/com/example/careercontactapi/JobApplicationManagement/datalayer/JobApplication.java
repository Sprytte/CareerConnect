package com.example.careercontactapi.JobApplicationManagement.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_posting_id", nullable = false)
    private Integer jobPostingId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "resume_id", nullable = false)
    private Integer resumeId;

    @Column(nullable = false)
    private String status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
}
