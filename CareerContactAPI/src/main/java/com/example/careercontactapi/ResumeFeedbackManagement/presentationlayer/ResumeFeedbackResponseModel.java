package com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer;

import java.util.List;

/** AI-generated feedback on one resume file. Nothing here is stored. */
public record ResumeFeedbackResponseModel(
        String fileName,
        String summary,
        List<String> strengths,
        List<String> improvements
) {
}
