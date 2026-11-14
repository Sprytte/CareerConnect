package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

/**
 * A resume ready to send for review: either the original PDF bytes, which the
 * AI service reads directly, or the plain text extracted from a DOCX file.
 */
public record ResumeDocument(String fileName, byte[] pdf, String text) {

    public static ResumeDocument ofPdf(String fileName, byte[] pdf) {
        return new ResumeDocument(fileName, pdf, null);
    }

    public static ResumeDocument ofText(String fileName, String text) {
        return new ResumeDocument(fileName, null, text);
    }

    public boolean isPdf() {
        return pdf != null;
    }
}
