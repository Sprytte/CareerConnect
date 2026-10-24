package com.example.careercontactapi.JobApplicationManagement.businesslayer;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplicationRepository;
import com.example.careercontactapi.JobApplicationManagement.presentationlayer.AccountApplicationResponseModel;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final JobApplicationRepository applications;
    private final JobPostingRepository postings;

    @Transactional(readOnly = true)
    public List<AccountApplicationResponseModel> getApplications(
            String authenticatedUserId
    ) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var ownApplications =
                applications.findAllByUserIdOrderByAppliedAtDescIdDesc(
                        authenticatedUserId
                );

        Map<Integer, JobPosting> jobs = postings
                .findAllById(
                        ownApplications.stream()
                                .map(application -> application.getJobPostingId())
                                .distinct()
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        JobPosting::getId,
                        Function.identity()
                ));

        return ownApplications.stream()
                .map(application -> {
                    var job = jobs.get(application.getJobPostingId());

                    return new AccountApplicationResponseModel(
                            application.getId(),
                            job == null
                                    ? "Job posting no longer available"
                                    : nonblank(
                                            job.getJobName(),
                                            "Job title unavailable"
                                    ),
                            job == null
                                    ? "Employer unavailable"
                                    : nonblank(
                                            job.getCompany(),
                                            "Employer unavailable"
                                    ),
                            displayStatus(
                                    application.getStatus().name()
                            ),
                            application.getAppliedAt() == null
                                    ? null
                                    : application.getAppliedAt().toLocalDate()
                    );
                })
                .toList();
    }

    private static String nonblank(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim();
    }

    static String displayStatus(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SUBMITTED", "APPLIED" -> "Applied";
            case "INTERVIEW" -> "Interview";
            case "OFFER", "OFFERED" -> "Offered";
            case "REJECT", "REJECTED" -> "Rejected";
            default -> value;
        };
    }
}