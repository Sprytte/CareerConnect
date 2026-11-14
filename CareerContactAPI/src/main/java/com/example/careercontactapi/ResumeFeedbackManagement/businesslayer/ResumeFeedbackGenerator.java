package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer.ResumeFeedbackResponseModel;

/**
 * The Generative AI service behind resume feedback. Keeping it behind this
 * interface means the provider can be replaced without touching the endpoint.
 */
public interface ResumeFeedbackGenerator {
    ResumeFeedbackResponseModel generate(ResumeDocument document);
}
