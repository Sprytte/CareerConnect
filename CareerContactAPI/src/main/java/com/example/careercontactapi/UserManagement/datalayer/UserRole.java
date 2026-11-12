package com.example.careercontactapi.UserManagement.datalayer;

import java.util.List;
import java.util.Locale;

/**
 * The account types a CareerConnect user can choose after signing in.
 *
 * <p>Permissions are intentionally simple, stable strings so the frontend can
 * adjust navigation while the backend remains the source of truth for access
 * control. Auth0 continues to own authentication; this role decides what the
 * authenticated account may do inside CareerConnect.</p>
 */
public enum UserRole {
    JOB_SEEKER(
            "Job seeker",
            List.of("jobs:read", "applications:manage", "resumes:manage")
    ),
    RECRUITER(
            "Recruiter",
            List.of("jobs:read", "job-postings:manage", "applicants:read")
    );

    private final String displayName;
    private final List<String> permissions;

    UserRole(String displayName, List<String> permissions) {
        this.displayName = displayName;
        this.permissions = List.copyOf(permissions);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Accept the enum name plus common user-facing spellings such as
     * {@code job-seeker} or {@code job seeker}.
     */
    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Choose whether you are a job seeker or a recruiter");
        }

        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose either Job seeker or Recruiter");
        }
    }
}
