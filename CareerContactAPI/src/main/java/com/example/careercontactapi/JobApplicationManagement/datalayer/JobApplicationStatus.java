package com.example.careercontactapi.JobApplicationManagement.datalayer;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum JobApplicationStatus {
    APPLIED("Applied"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    REJECT("Reject");

    private final String value;

    JobApplicationStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<JobApplicationStatus> from(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalizedValue))
                .findFirst();
    }
}
