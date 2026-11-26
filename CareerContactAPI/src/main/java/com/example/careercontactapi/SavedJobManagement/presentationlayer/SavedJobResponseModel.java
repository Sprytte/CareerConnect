package com.example.careercontactapi.SavedJobManagement.presentationlayer;

import com.example.careercontactapi.JobPostingManagement.presentationlayer.JobPostingResponseModel;

import java.time.LocalDateTime;

/**
 * One saved job of the signed-in user. job is null when the posting has been
 * deleted since it was saved, so the entry can still be shown and removed.
 */
public record SavedJobResponseModel(
        Integer jobId,
        LocalDateTime savedAt,
        JobPostingResponseModel job
) {
}
