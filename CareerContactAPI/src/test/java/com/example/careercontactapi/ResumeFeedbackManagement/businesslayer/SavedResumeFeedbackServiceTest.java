package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer.ResumeFeedbackResponseModel;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SavedResumeFeedbackServiceTest {

    private static final byte[] PDF = "%PDF-1.7 fixture".getBytes(StandardCharsets.US_ASCII);
    private static final String USER = "auth0|one";

    private final List<ResumeDocument> received = new ArrayList<>();
    private final ResumeService resumes = mock(ResumeService.class);
    private final ResumeFeedbackService service = new ResumeFeedbackService(document -> {
        received.add(document);
        return new ResumeFeedbackResponseModel(
                document.fileName(), "Summary", List.of("Strength"), List.of("Improvement"));
    }, resumes);

    private static ResumeFile stored(String name, byte[] content) {
        return new ResumeFile(new ByteArrayResource(content), name, "application/octet-stream");
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void sendsTheOwnersSavedPdfToTheGenerator() {
        when(resumes.downloadResumeForUser(7, USER)).thenReturn(stored("resume.pdf", PDF));

        var feedback = service.requestFeedbackForResume(USER, 7);

        assertEquals("resume.pdf", feedback.fileName());
        assertEquals("Summary", feedback.summary());
        assertEquals(1, received.size());
        assertTrue(received.get(0).isPdf());
        assertArrayEquals(PDF, received.get(0).pdf());
        verify(resumes).downloadResumeForUser(7, USER);
    }

    @Test
    void sendsTheTextOfASavedDocxToTheGenerator() throws IOException {
        byte[] docx = DocxTextExtractorTest.docxWithBody(
                "<w:p><w:r><w:t>Backend developer, Montreal</w:t></w:r></w:p>");
        when(resumes.downloadResumeForUser(8, USER)).thenReturn(stored("resume.docx", docx));

        service.requestFeedbackForResume(USER, 8);

        assertEquals(1, received.size());
        assertFalse(received.get(0).isPdf());
        assertEquals("Backend developer, Montreal", received.get(0).text());
    }

    @Test
    void keepsOnlyTheFileNameFromAStoredPath() {
        when(resumes.downloadResumeForUser(9, USER))
                .thenReturn(stored("C:\\Users\\arley\\Documents\\cv.pdf", PDF));

        assertEquals("cv.pdf", service.requestFeedbackForResume(USER, 9).fileName());
    }

    @Test
    void requiresAnAuthenticatedUserBeforeLookingUpAResume() {
        assertEquals(401, statusOf(() -> service.requestFeedbackForResume(null, 1)));
        assertEquals(401, statusOf(() -> service.requestFeedbackForResume(" ", 1)));
        verifyNoInteractions(resumes);
        assertTrue(received.isEmpty());
    }

    @Test
    void rejectsMissingAndInvalidResumeIds() {
        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, null)));
        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, 0)));
        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, -1)));
        verifyNoInteractions(resumes);
    }

    @Test
    void propagatesNotFoundAndForbiddenFromTheResumeService() {
        when(resumes.downloadResumeForUser(1, USER))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
        when(resumes.downloadResumeForUser(2, USER))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resume"));

        assertEquals(404, statusOf(() -> service.requestFeedbackForResume(USER, 1)));
        assertEquals(403, statusOf(() -> service.requestFeedbackForResume(USER, 2)));
        assertTrue(received.isEmpty());
    }

    @Test
    void reportsAMissingStoredFileWhenTheResumeServiceReturnsNothing() {
        assertEquals(404, statusOf(() -> service.requestFeedbackForResume(USER, 1)));
        assertTrue(received.isEmpty());
    }

    @Test
    void rejectsSavedFilesThatAreEmptyUnsupportedOrNotReallyPdf() {
        when(resumes.downloadResumeForUser(1, USER)).thenReturn(stored("resume.pdf", new byte[0]));
        when(resumes.downloadResumeForUser(2, USER)).thenReturn(stored("resume.txt", PDF));
        when(resumes.downloadResumeForUser(3, USER))
                .thenReturn(stored("resume.pdf", "<html>not a pdf</html>".getBytes(StandardCharsets.US_ASCII)));

        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, 1)));
        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, 2)));
        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, 3)));
        assertTrue(received.isEmpty());
    }

    @Test
    void rejectsASavedFileLargerThanTheUploadLimit() {
        when(resumes.downloadResumeForUser(1, USER)).thenReturn(stored(
                "resume.pdf", new byte[(int) ResumeFeedbackService.MAX_FILE_SIZE + 1]));

        assertEquals(400, statusOf(() -> service.requestFeedbackForResume(USER, 1)));
        assertTrue(received.isEmpty());
    }

    @Test
    void reportsASavedDocxWithNoReadableText() throws IOException {
        byte[] empty = DocxTextExtractorTest.docxWithBody("<w:p/>");
        when(resumes.downloadResumeForUser(1, USER)).thenReturn(stored("resume.docx", empty));

        assertEquals(422, statusOf(() -> service.requestFeedbackForResume(USER, 1)));
        assertTrue(received.isEmpty());
    }
}
