
package com.example.careercontactapi.JobPostingManagement.businesslayer;

import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobPostingServiceImplTest {

    private JobPostingRepository jobPostingRepository;
    private JobPostingServiceImpl jobPostingService;

    @BeforeEach
    void setUp() {
        jobPostingRepository = mock(JobPostingRepository.class);
        jobPostingService = new JobPostingServiceImpl(jobPostingRepository);
    }

    @Test
    void archiveJobPosting_success() {
        JobPosting job = JobPosting.builder()
                .id(1)
                .userId("recruiter1")
                .archived(false)
                .build();

        when(jobPostingRepository.findById(1)).thenReturn(Optional.of(job));
        when(jobPostingRepository.save(any(JobPosting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = jobPostingService.archiveJobPosting(1, "recruiter1");

        assertTrue(result.isArchived());
        assertTrue(job.isArchived());
        verify(jobPostingRepository).save(job);
    }

    @Test
    void archiveJobPosting_wrongOwner() {
        JobPosting job = JobPosting.builder()
                .id(1)
                .userId("recruiter1")
                .build();

        when(jobPostingRepository.findById(1)).thenReturn(Optional.of(job));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> jobPostingService.archiveJobPosting(1, "recruiter2")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void archiveJobPosting_notFound() {
        when(jobPostingRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> jobPostingService.archiveJobPosting(99, "recruiter1")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void archiveJobPosting_alreadyArchived() {
        JobPosting job = JobPosting.builder()
                .id(1)
                .userId("recruiter1")
                .archived(true)
                .build();

        when(jobPostingRepository.findById(1)).thenReturn(Optional.of(job));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> jobPostingService.archiveJobPosting(1, "recruiter1")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void getJobPostings_excludesArchivedJobs() {
        JobPosting activeJob = JobPosting.builder()
                .id(1)
                .archived(false)
                .build();

        JobPosting archivedJob = JobPosting.builder()
                .id(2)
                .archived(true)
                .build();

        when(jobPostingRepository.findAll())
                .thenReturn(List.of(activeJob, archivedJob));

        var result = jobPostingService.getJobPostings();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }
}