package com.example.careercontactapi.MailManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;

public interface EmailService {
    String sendMail(String email);
    void sendUpdatedJobApplicationAlert(JobApplication jobApplication);
    void sendUpcomingInterviewReminder(JobApplication jobApplication, JobPosting jobPosting);
    void sendUpcomingDeadlineReminder(JobApplication jobApplication);
    void sendNewApplicantAlert(JobApplication jobApplication, JobPosting jobPosting);
}
