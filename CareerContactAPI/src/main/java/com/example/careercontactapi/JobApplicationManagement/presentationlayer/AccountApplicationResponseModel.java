package com.example.careercontactapi.JobApplicationManagement.presentationlayer;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the signed-in job seeker's application list.
 *
 * resumeId, deadline and modifiable let the interface offer editing and
 * withdrawal only while the posting is still open. The server remains the
 * authority: PUT and DELETE re-check ownership and the deadline.
 */
public record AccountApplicationResponseModel(
        Integer id, String jobTitle, String company, String status, LocalDate appliedOn,
        Integer resumeId, LocalDateTime deadline, boolean modifiable
) {}
