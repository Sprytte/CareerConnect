package com.example.careercontactapi.JobPostingManagement.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@AllArgsConstructor()
public class JobPostingRequestModel {
    String userId;
    String jobName;
    String company;
    String description;
    String location;
    String category;
    String salary;
    LocalDateTime deadline;
}
