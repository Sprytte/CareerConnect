package com.example.careercontactapi.JobPostingManagement.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "job_name")
    private String jobName;

    private String company;

    private String description;

    private String location;

    private String category;

    private String salary;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    private LocalDateTime deadline;
}