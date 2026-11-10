package com.example.careercontactapi.JobApplicationManagement;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.AccountApplicationService;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.AccountApplicationRequestModel;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.ResumeManagement.datalayer.Resume;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task 14.2: the application list tells the interface whether an application
 * can still be edited or withdrawn, and the write endpoints enforce the same
 * ownership and deadline rules.
 */
class AccountApplicationChangeTest {
    private final JobApplicationRepository applications = mock(JobApplicationRepository.class);
    private final JobPostingRepository postings = mock(JobPostingRepository.class);
    private final ResumeRepository resumes = mock(ResumeRepository.class);
    private final AccountApplicationService service =
            new AccountApplicationService(applications, postings, resumes);

    private JobApplication application() {
        return JobApplication.builder().id(1).jobPostingId(20).userId("user|one").resumeId(5)
                .status("Applied").appliedAt(LocalDateTime.of(2026, 11, 1, 9, 0)).build();
    }

    private JobPosting posting(LocalDateTime deadline) {
        return JobPosting.builder().id(20).jobName("Developer").company("Fixture employer")
                .deadline(deadline).build();
    }

    private void storedApplicationWithDeadline(LocalDateTime deadline) {
        when(applications.findById(1)).thenReturn(Optional.of(application()));
        when(postings.findById(20)).thenReturn(Optional.of(posting(deadline)));
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void listMarksAnApplicationModifiableWhileThePostingIsOpen() {
        var deadline = LocalDateTime.now().plusDays(3);
        when(applications.findAllByUserIdOrderByAppliedAtDescIdDesc("user|one")).thenReturn(List.of(application()));
        when(postings.findAllById(anyList())).thenReturn(List.of(posting(deadline)));

        var item = service.getApplications("user|one").get(0);

        assertEquals(5, item.resumeId());
        assertEquals(deadline, item.deadline());
        assertTrue(item.modifiable());
    }

    @Test
    void listMarksAnApplicationLockedAfterTheDeadline() {
        when(applications.findAllByUserIdOrderByAppliedAtDescIdDesc("user|one")).thenReturn(List.of(application()));
        when(postings.findAllById(anyList())).thenReturn(List.of(posting(LocalDateTime.now().minusMinutes(1))));

        assertFalse(service.getApplications("user|one").get(0).modifiable());
    }

    @Test
    void listMarksAnApplicationLockedWhenThePostingWasDeleted() {
        when(applications.findAllByUserIdOrderByAppliedAtDescIdDesc("user|one")).thenReturn(List.of(application()));
        when(postings.findAllById(anyList())).thenReturn(List.of());

        var item = service.getApplications("user|one").get(0);

        assertNull(item.deadline());
        assertFalse(item.modifiable());
    }

    @Test
    void editReplacesTheResumeBeforeTheDeadline() {
        storedApplicationWithDeadline(LocalDateTime.now().plusDays(1));
        when(resumes.findById(9)).thenReturn(Optional.of(Resume.builder().id(9).userId("user|one").build()));

        var updated = service.updateApplication("user|one", 1, new AccountApplicationRequestModel(9));

        assertEquals(9, updated.resumeId());
        assertTrue(updated.modifiable());
        verify(applications).save(any(JobApplication.class));
    }

    @Test
    void editIsRejectedAfterTheDeadline() {
        storedApplicationWithDeadline(LocalDateTime.now().minusDays(1));

        assertEquals(400, statusOf(() ->
                service.updateApplication("user|one", 1, new AccountApplicationRequestModel(9))));
        verify(applications, never()).save(any(JobApplication.class));
    }

    @Test
    void editRejectsAResumeOwnedBySomeoneElse() {
        storedApplicationWithDeadline(LocalDateTime.now().plusDays(1));
        when(resumes.findById(9)).thenReturn(Optional.of(Resume.builder().id(9).userId("user|two").build()));

        assertEquals(403, statusOf(() ->
                service.updateApplication("user|one", 1, new AccountApplicationRequestModel(9))));
        verify(applications, never()).save(any(JobApplication.class));
    }

    @Test
    void anotherAccountCannotEditOrWithdrawTheApplication() {
        storedApplicationWithDeadline(LocalDateTime.now().plusDays(1));

        assertEquals(403, statusOf(() ->
                service.updateApplication("user|two", 1, new AccountApplicationRequestModel(9))));
        assertEquals(403, statusOf(() -> service.deleteApplication("user|two", 1)));
        verify(applications, never()).delete(any(JobApplication.class));
    }

    @Test
    void withdrawDeletesTheApplicationBeforeTheDeadline() {
        storedApplicationWithDeadline(LocalDateTime.now().plusDays(1));

        service.deleteApplication("user|one", 1);

        verify(applications).delete(any(JobApplication.class));
    }

    @Test
    void withdrawIsRejectedAfterTheDeadline() {
        storedApplicationWithDeadline(LocalDateTime.now().minusDays(1));

        assertEquals(400, statusOf(() -> service.deleteApplication("user|one", 1)));
        verify(applications, never()).delete(any(JobApplication.class));
    }
}
