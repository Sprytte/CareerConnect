package com.example.careercontactapi.MailManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Properties;


@Slf4j
@Transactional
@Service
public class EmailServiceImpl implements EmailService{
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;
    @Value("${spring.mail.username}")
    private String sender;
    Session session;

    @Generated
    public EmailServiceImpl(@Value("${spring.mail.username}") String username,
                            @Value("${spring.mail.password}") String password,
                            JavaMailSender javaMailSender,
                            UserRepository userRepository,
                            JobPostingRepository jobPostingRepository) {

        this.mailSender = javaMailSender;
        this.userRepository = userRepository;
        this.jobPostingRepository = jobPostingRepository;

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(prop,
                new Authenticator() {
                    @Generated
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
    }

    @Override
    public String sendMail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Email subject");
        message.setText("Email body");

        mailSender.send(message);
        return "Success!";
    }

    @Override
    public void sendUpdatedJobApplicationAlert(JobApplication jobApplication) {
        User user = userRepository.findByUserId(jobApplication.getUserId()).orElseThrow(() ->
            new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
        ));
        JobPosting jobPosting = jobPostingRepository.getReferenceById(jobApplication.getJobPostingId());

        MimeMessage message = mailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(MimeMessage.RecipientType.TO, user.getEmail());
            message.setSubject("Your application has been updated!");

            InputStream resource = new ClassPathResource("/templates/ApplicationUpdate.html").getInputStream();
            String htmlTemplate = IOUtils.toString(resource, "UTF-8");

            htmlTemplate = htmlTemplate.replace("${username}", user.getName());
            htmlTemplate = htmlTemplate.replace("${jobName}", jobPosting.getJobName());
            htmlTemplate = htmlTemplate.replace("${jobStatus}", jobApplication.getStatus());
            htmlTemplate = htmlTemplate.replace("${applicationId}", Integer.toString(jobApplication.getId()));

            message.setContent(htmlTemplate, "text/html; charset=utf-8");
        } catch (Exception e){
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }

        mailSender.send(message);
    }

    @Override
    public void sendUpcomingInterviewReminder(JobApplication jobApplication, JobPosting jobPosting) {
        User applicant = userRepository.findByUserId(jobApplication.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        User recruiter = userRepository.findByUserId(jobPosting.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
        ));

        MimeMessage message = mailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(MimeMessage.RecipientType.TO, applicant.getEmail());
            message.setSubject("You have an upcoming interview!");

            InputStream resource = new ClassPathResource("/templates/InterviewReminder.html").getInputStream();
            String htmlTemplate = IOUtils.toString(resource, "UTF-8");

            htmlTemplate = htmlTemplate.replace("${username}", applicant.getName());
            htmlTemplate = htmlTemplate.replace("${jobName}", jobPosting.getJobName());
            htmlTemplate = htmlTemplate.replace("${interviewTime}", jobApplication.getInterviewTimeSlot().toString());
            htmlTemplate = htmlTemplate.replace("${applicationId}", Integer.toString(jobApplication.getId()));

            message.setContent(htmlTemplate, "text/html; charset=utf-8");
        } catch (Exception e){
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }

        mailSender.send(message);

        try {
            message.setRecipients(MimeMessage.RecipientType.TO, recruiter.getEmail());

            InputStream resource = new ClassPathResource("/templates/InterviewReminder.html").getInputStream();
            String htmlTemplate = IOUtils.toString(resource, "UTF-8");

            htmlTemplate = htmlTemplate.replace("${username}", recruiter.getName());
            htmlTemplate = htmlTemplate.replace("${jobName}", jobPosting.getJobName());
            htmlTemplate = htmlTemplate.replace("${interviewTime}", jobApplication.getInterviewTimeSlot().toString());
            htmlTemplate = htmlTemplate.replace("${applicationId}", Integer.toString(jobApplication.getId()));

            message.setContent(htmlTemplate, "text/html; charset=utf-8");
        } catch (Exception e){
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }

        mailSender.send(message);
    }

    @Override
    public void sendUpcomingDeadlineReminder(JobApplication jobApplication) {
        User user = userRepository.findByUserId(jobApplication.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        JobPosting jobPosting = jobPostingRepository.getReferenceById(jobApplication.getJobPostingId());

        MimeMessage message = mailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(MimeMessage.RecipientType.TO, user.getEmail());
            message.setSubject("Upcoming deadline!");

            InputStream resource = new ClassPathResource("/templates/DeadlineReminder.html").getInputStream();
            String htmlTemplate = IOUtils.toString(resource, "UTF-8");

            htmlTemplate = htmlTemplate.replace("${username}", user.getName());
            htmlTemplate = htmlTemplate.replace("${jobName}", jobPosting.getJobName());
            htmlTemplate = htmlTemplate.replace("${applicationId}", Integer.toString(jobApplication.getId()));

            message.setContent(htmlTemplate, "text/html; charset=utf-8");
        } catch (Exception e){
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }

        mailSender.send(message);
    }

    @Override
    public void sendNewApplicantAlert(JobApplication jobApplication, JobPosting jobPosting) {
        User applicant = userRepository.findByUserId(jobApplication.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        User recruiter = userRepository.findByUserId(jobPosting.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        MimeMessage message = mailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(MimeMessage.RecipientType.TO, recruiter.getEmail());
            message.setSubject("Your application has been updated!");

            InputStream resource = new ClassPathResource("/templates/ApplicationUpdate.html").getInputStream();
            String htmlTemplate = IOUtils.toString(resource, "UTF-8");

            htmlTemplate = htmlTemplate.replace("${username}", recruiter.getName());
            htmlTemplate = htmlTemplate.replace("${jobName}", jobPosting.getJobName());
            htmlTemplate = htmlTemplate.replace("${jobId}", Integer.toString(jobPosting.getId()));
            htmlTemplate = htmlTemplate.replace("${applicantName}", applicant.getName());

            message.setContent(htmlTemplate, "text/html; charset=utf-8");
        } catch (Exception e){
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }

        mailSender.send(message);
    }

}
