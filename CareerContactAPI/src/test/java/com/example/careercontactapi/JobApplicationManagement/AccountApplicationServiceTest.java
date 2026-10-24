package com.example.careercontactapi.JobApplicationManagement;

import com.example.careercontactapi.JobApplicationManagement.businesslayer.AccountApplicationService;
import com.example.careercontactapi.JobApplicationManagement.datalayer.ApplicationStatus;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.AccountApplicationController;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AccountApplicationServiceTest {

    private final JobApplicationRepository applications =
            mock(JobApplicationRepository.class);

    private final JobPostingRepository postings =
            mock(JobPostingRepository.class);

    private final AccountApplicationService service =
            new AccountApplicationService(applications, postings);

    private JobApplication application(
            int id,
            ApplicationStatus status
    ) {
        return JobApplication.builder()
                .id(id)
                .jobPostingId(20)
                .userId("user|one")
                .status(status)
                .appliedAt(
                        LocalDateTime.of(
                                2026,
                                10,
                                21,
                                23,
                                59
                        )
                )
                .build();
    }

    @Test
    void onlyQueriesApplicationsOfAuthenticatedUserAndIncludesJobDetails() {

        when(
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                )
        ).thenReturn(
                List.of(
                        application(
                                1,
                                ApplicationStatus.APPLIED
                        )
                )
        );

        when(postings.findAllById(anyList()))
                .thenReturn(
                        List.of(
                                JobPosting.builder()
                                        .id(20)
                                        .jobName("Developer")
                                        .company("Fixture employer")
                                        .build()
                        )
                );

        var result = service.getApplications("user|one");

        assertEquals(1, result.size());
        assertEquals("Applied", result.get(0).status());
        assertEquals("Developer", result.get(0).jobTitle());
        assertEquals(
                "2026-10-21",
                result.get(0).appliedOn().toString()
        );

        verify(applications)
                .findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                );

        verify(postings)
                .findAllById(List.of(20));
    }

    @Test
    void mapsRequiredApplicationStatusesCorrectly() {

        var statuses = Arrays.asList(
                ApplicationStatus.APPLIED,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.OFFERED,
                ApplicationStatus.REJECTED
        );

        var expected = Arrays.asList(
                "Applied",
                "Interview",
                "Offered",
                "Rejected"
        );

        var records = new ArrayList<JobApplication>();

        for (int i = 0; i < statuses.size(); i++) {
            records.add(
                    application(
                            i + 1,
                            statuses.get(i)
                    )
            );
        }

        when(
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                )
        ).thenReturn(records);

        when(postings.findAllById(anyList()))
                .thenReturn(List.of());

        assertEquals(
                expected,
                service.getApplications("user|one")
                        .stream()
                        .map(x -> x.status())
                        .toList()
        );
    }

    @Test
    void deletedPostingDoesNotHideApplicationHistory() {

        when(
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                )
        ).thenReturn(
                List.of(
                        application(
                                1,
                                ApplicationStatus.INTERVIEW
                        )
                )
        );

        when(postings.findAllById(anyList()))
                .thenReturn(List.of());

        var item = service
                .getApplications("user|one")
                .get(0);

        assertEquals(
                "Job posting no longer available",
                item.jobTitle()
        );

        assertEquals(
                "Employer unavailable",
                item.company()
        );
    }

    @Test
    void emptyAccountReturnsConfirmedEmptyList() {

        when(
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                )
        ).thenReturn(List.of());

        when(postings.findAllById(anyList()))
                .thenReturn(List.of());

        assertTrue(
                service.getApplications("user|one").isEmpty()
        );
    }

    @Test
    void incompletePostingDoesNotHideTheApplicationList() {

        when(
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        "user|one"
                )
        ).thenReturn(
                List.of(
                        application(
                                1,
                                ApplicationStatus.APPLIED
                        )
                )
        );

        when(postings.findAllById(anyList()))
                .thenReturn(
                        List.of(
                                JobPosting.builder()
                                        .id(20)
                                        .jobName(" ")
                                        .company(null)
                                        .build()
                        )
                );

        var item = service
                .getApplications("user|one")
                .get(0);

        assertEquals(
                "Job title unavailable",
                item.jobTitle()
        );

        assertEquals(
                "Employer unavailable",
                item.company()
        );
    }

    @Test
    void absentIdentityCannotReachRepository() {

        assertEquals(
                401,
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.getApplications(null)
                )
                        .getStatusCode()
                        .value()
        );

        verifyNoInteractions(
                applications,
                postings
        );
    }

    @Test
    void controllerRequiresSessionAndDerivesIdentityFromPrincipal() {

        var backend =
                mock(AccountApplicationService.class);

        var controller =
                new AccountApplicationController(backend);

        assertEquals(
                401,
                controller.getApplications(null)
                        .getStatusCode()
                        .value()
        );

        verifyNoInteractions(backend);

        var principal =
                mock(OidcUser.class);

        when(principal.getSubject())
                .thenReturn("user|one");

        when(
                backend.getApplications("user|one")
        ).thenReturn(List.of());

        var response =
                controller.getApplications(principal);

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                "no-store",
                response.getHeaders().getCacheControl()
        );

        verify(backend)
                .getApplications("user|one");
    }
}