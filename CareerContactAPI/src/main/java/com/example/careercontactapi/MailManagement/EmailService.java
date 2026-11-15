package com.example.careercontactapi.MailManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;

public interface EmailService {
    String sendMail(String email);
    void sendUpdatedJobApplicationAlert(JobApplication jobApplication);
}
