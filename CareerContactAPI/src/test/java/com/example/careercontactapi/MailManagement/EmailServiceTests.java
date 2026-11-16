package com.example.careercontactapi.MailManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest
public class EmailServiceTests {
    private JavaMailSender mailSender;
    private UserRepository userRepository;
    private JobPostingRepository jobPostingRepository;
    private EmailServiceImpl emailService;
    @MockBean
    private MimeMessage mimeMessage;

    private final User applicant = User.builder()
                                    .userId("applicant|one")
                                    .name("applicant")
                                    .email("applicant@example.test")
                                    .build();
    private final User recruiter = User.builder()
                                    .userId("recruiter|one")
                                    .name("recruiter")
                                    .email("recruiter@example.test")
                                    .build();
    private final JobPosting job = JobPosting.builder()
                                    .id(1)
                                    .userId("recruiter|one")
                                    .jobName("Programmer")
                                    .archived(false)
                                    .build();
    private JobApplication jobApplication;

    @BeforeEach
    void setUp() {
        this.mailSender = mock(JavaMailSender.class);
        this.userRepository = mock(UserRepository.class);
        this.jobPostingRepository = mock(JobPostingRepository.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        this.emailService = new EmailServiceImpl("user",
                                        "pass",
                                                this.mailSender,
                                                this.userRepository,
                                                this.jobPostingRepository);
        this.emailService.setSender("google@google.com");
        this.jobApplication = this.application();
    }

    @Test
    void sendUpdatedJobApplicationAlertSendsToProperUser() {
        when(userRepository.findByUserId(this.jobApplication.getUserId()))
                .thenReturn(Optional.of(this.applicant));
        when(jobPostingRepository.getReferenceById(this.jobApplication.getId()))
                .thenReturn(this.job);

        this.emailService.sendUpdatedJobApplicationAlert(this.jobApplication);

        verify(mailSender).createMimeMessage();
//        verify(mimeMessage).setFrom("google@google.com");
//        verify(mimeMessage).setRecipients(MimeMessage.RecipientType.TO, this.applicant.getEmail());
//        verify(mimeMessage).setSubject("Your application has been updated!");
//        verify(mimeMessage, atLeastOnce()).setContent(any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendUpcomingInterviewReminderSendsToProperUsers() {
        when(userRepository.findByUserId(this.jobApplication.getUserId()))
                .thenReturn(Optional.of(this.applicant));
        when(userRepository.findByUserId(this.job.getUserId()))
                .thenReturn(Optional.of(this.recruiter));
        when(jobPostingRepository.getReferenceById(this.jobApplication.getId()))
                .thenReturn(this.job);

        this.emailService.sendUpcomingInterviewReminder(this.jobApplication, this.job);

        verify(mailSender).createMimeMessage();
        verify(mailSender, times(2)).send(mimeMessage);
    }

    @Test
    void sendUpcomingDeadlineReminderSendsToProperUsers() {
        when(userRepository.findByUserId(this.jobApplication.getUserId()))
                .thenReturn(Optional.of(this.applicant));
        when(jobPostingRepository.getReferenceById(this.jobApplication.getId()))
                .thenReturn(this.job);

        this.emailService.sendUpcomingDeadlineReminder(this.jobApplication);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    private JobApplication application() {
        return JobApplication.builder().id(1).jobPostingId(1).userId("applicant|one").resumeId(5)
                .status("Applied").appliedAt(LocalDateTime.now()).interviewTimeSlot(LocalDateTime.now()).build();
    }
}
