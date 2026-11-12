package com.example.careercontactapi.UserManagement.presentationlayer;

/**
 * Request body for choosing a new account's role. The value accepts the API
 * constants ({@code JOB_SEEKER}, {@code RECRUITER}) and common spellings such
 * as {@code job seeker}.
 */
public record RoleUpdateRequestModel(String role) {
}
