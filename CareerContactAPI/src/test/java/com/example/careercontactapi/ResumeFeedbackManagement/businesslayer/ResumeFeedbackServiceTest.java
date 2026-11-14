package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer.ResumeFeedbackResponseModel;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeFeedbackServiceTest {

    private static final byte[] PDF = "%PDF-1.7 fixture".getBytes(StandardCharsets.US_ASCII);

    private final List<ResumeDocument> received = new ArrayList<>();
    private final ResumeFeedbackService service = new ResumeFeedbackService(document -> {
        received.add(document);
        return new ResumeFeedbackResponseModel(document.fileName(), "Summary", List.of("Strength"), List.of("Improvement"));
    });

    private static MockMultipartFile upload(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void sendsAPdfToTheGeneratorUnchanged() {
        var feedback = service.requestFeedback("user|one", upload("Resume.PDF", PDF));

        assertEquals("Resume.PDF", feedback.fileName());
        assertEquals("Summary", feedback.summary());
        assertEquals(1, received.size());
        assertTrue(received.get(0).isPdf());
        assertArrayEquals(PDF, received.get(0).pdf());
    }

    @Test
    void sendsTheTextOfADocxToTheGenerator() throws IOException {
        byte[] docx = DocxTextExtractorTest.docxWithBody("<w:p><w:r><w:t>Frontend developer</w:t></w:r></w:p>");

        service.requestFeedback("user|one", upload("resume.docx", docx));

        assertFalse(received.get(0).isPdf());
        assertEquals("Frontend developer", received.get(0).text());
    }

    @Test
    void keepsOnlyTheFileNameFromAPath() {
        var feedback = service.requestFeedback("user|one", upload("C:\\Users\\arley\\Documents\\cv.pdf", PDF));

        assertEquals("cv.pdf", feedback.fileName());
    }

    @Test
    void requiresAnAuthenticatedUser() {
        assertEquals(401, statusOf(() -> service.requestFeedback(null, upload("resume.pdf", PDF))));
        assertEquals(401, statusOf(() -> service.requestFeedback(" ", upload("resume.pdf", PDF))));
        assertTrue(received.isEmpty());
    }

    @Test
    void rejectsMissingEmptyOversizedAndUnsupportedFiles() {
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", null)));
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", upload("resume.pdf", new byte[0]))));
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one",
                upload("resume.pdf", new byte[(int) ResumeFeedbackService.MAX_FILE_SIZE + 1]))));
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", upload("resume.txt", PDF))));
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", upload("resume", PDF))));
        assertTrue(received.isEmpty());
    }

    @Test
    void rejectsFilesWhoseContentDoesNotMatchTheExtension() {
        byte[] notPdf = "<html>not a pdf</html>".getBytes(StandardCharsets.US_ASCII);

        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", upload("resume.pdf", notPdf))));
        assertEquals(400, statusOf(() -> service.requestFeedback("user|one", upload("resume.docx", PDF))));
        assertTrue(received.isEmpty());
    }

    @Test
    void reportsADocxWithNoReadableText() throws IOException {
        byte[] empty = DocxTextExtractorTest.docxWithBody("<w:p/>");

        assertEquals(422, statusOf(() -> service.requestFeedback("user|one", upload("resume.docx", empty))));
        assertTrue(received.isEmpty());
    }
}
