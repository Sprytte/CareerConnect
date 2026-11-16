package com.example.careercontactapi.Scheduler;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.MailManagement.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeadlineScheduler {
    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final EmailService emailService;

    // Checks every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void checkIfDeadlinePassed() {
        LocalDateTime now = LocalDateTime.now();

        // Archive job postings whose deadline has passed
        List<JobPosting> oldJobPostings = jobPostingRepository.findAll();
        oldJobPostings.stream().filter(j -> !j.isArchived()).forEach(j -> {
            if (j.getDeadline().isBefore(now)) {
                j.setArchived(true);
                jobPostingRepository.save(j);
            }
        });

        // Alert job seekers when one of the job postings they applied to is nearing the deadline
        List<JobPosting> jobPostings = jobPostingRepository.findAll();
        jobPostings.stream().filter(j -> !j.isArchived() && !j.isDeadlineAlerted()).forEach(j -> {
            if (now.until(j.getDeadline(), ChronoUnit.HOURS) <= 24) {
                jobApplicationRepository.findAllByJobPostingId(j.getId())
                        .forEach(emailService::sendUpcomingDeadlineReminder);
                j.setDeadlineAlerted(true);
                jobPostingRepository.save(j);
            }
        });

        // Alert recruiters and job seekers of upcoming interviews
        List<JobApplication> jobApplications = jobApplicationRepository.findAll();
        jobApplications.stream().filter(j ->
                j.getStatus().equals("Interview") &&
                j.getInterviewTimeSlot() != null &&
                !j.isInterviewAlerted() &&
                !jobPostingRepository.findById(j.getJobPostingId()).orElseThrow().isArchived())
                .forEach(j -> {
                    if (now.until(j.getInterviewTimeSlot(), ChronoUnit.HOURS) <= 24) {
                        j.setInterviewAlerted(true);
                        emailService.sendUpcomingInterviewReminder(
                                j, jobPostingRepository.findById(j.getJobPostingId()).get()
                        );
                    }
        });
    }
}
