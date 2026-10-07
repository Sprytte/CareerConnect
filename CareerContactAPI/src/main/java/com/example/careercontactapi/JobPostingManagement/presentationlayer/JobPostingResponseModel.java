package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingResponseModel {
    Integer id;
    String userId;
    String jobName;
    String company;
    String description;
    String location;
    String category;
    String salary;
    LocalDateTime dateCreated;
    LocalDateTime deadline;
}