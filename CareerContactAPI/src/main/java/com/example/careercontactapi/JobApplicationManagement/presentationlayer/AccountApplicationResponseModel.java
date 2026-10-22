package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import java.time.LocalDate;

public record AccountApplicationResponseModel(
        Integer id, String jobTitle, String company, String status, LocalDate appliedOn
) {}
