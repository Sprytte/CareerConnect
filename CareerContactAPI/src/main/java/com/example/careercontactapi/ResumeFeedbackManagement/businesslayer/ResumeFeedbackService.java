package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer.ResumeFeedbackResponseModel;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Validates a resume, prepares it for the AI service and returns the generated
 * feedback. Uploaded files are processed in memory and never stored; the
 * saved-resume path reads a file the authenticated user already owns.
 */
@Service
public class ResumeFeedbackService {

    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ResumeFeedbackGenerator generator;
    private final ResumeService resumeService;

    @Autowired
    public ResumeFeedbackService(ResumeFeedbackGenerator generator, ResumeService resumeService) {
        this.generator = generator;
        this.resumeService = resumeService;
    }

    /**
     * Kept for callers that only use the multipart path. Production always
     * creates the service with the two-argument constructor above.
     */
    public ResumeFeedbackService(ResumeFeedbackGenerator generator) {
        this(generator, null);
    }

    public ResumeFeedbackResponseModel requestFeedback(String authenticatedUserId, MultipartFile file) {
        requireAuthenticated(authenticatedUserId);
        return generator.generate(toDocument(file));
    }

    /**
     * Task 10.1: review one of the authenticated user's saved resumes. The
     * ownership check happens in {@link ResumeService} before any file is read.
     */
    public ResumeFeedbackResponseModel requestFeedbackForResume(
            String authenticatedUserId,
            Integer resumeId
    ) {
        requireAuthenticated(authenticatedUserId);
        if (resumeId == null || resumeId <= 0) {
            throw badRequest("A valid resume ID is required");
        }
        if (resumeService == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Resume storage is not available"
            );
        }

        ResumeFile resume = resumeService.downloadResumeForUser(resumeId, authenticatedUserId);
        if (resume == null || resume.resource() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume file not found");
        }
        return generator.generate(toDocument(resume));
    }

    private ResumeDocument toDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("File size must not exceed 5 MB");
        }

        String fileName = displayName(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw badRequest("The uploaded file could not be read");
        }
        return toDocument(fileName, bytes, true);
    }

    private ResumeDocument toDocument(ResumeFile resume) {
        String fileName = displayName(resume.originalName());
        byte[] bytes;
        try (InputStream input = resume.resource().getInputStream()) {
            bytes = readBounded(input);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The saved resume could not be read",
                    e
            );
        }
        return toDocument(fileName, bytes, false);
    }

    private ResumeDocument toDocument(String fileName, byte[] bytes, boolean uploaded) {
        if (bytes == null || bytes.length == 0) {
            throw badRequest("File is required");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw badRequest("File size must not exceed 5 MB");
        }

        String extension = extension(fileName);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw badRequest("Only PDF and DOCX files are supported");
        }

        if (extension.equals("pdf")) {
            if (!startsWith(bytes, PDF_SIGNATURE)) {
                throw badRequest(uploaded
                        ? "The uploaded file is not a valid PDF"
                        : "The saved resume is not a valid PDF");
            }
            return ResumeDocument.ofPdf(fileName, bytes);
        }

        String text;
        try {
            text = DocxTextExtractor.extract(bytes);
        } catch (IOException e) {
            throw badRequest(uploaded
                    ? "The uploaded file is not a valid DOCX document"
                    : "The saved resume is not a valid DOCX document");
        }
        if (text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No readable text was found in the document"
            );
        }
        return ResumeDocument.ofText(fileName, text);
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_FILE_SIZE) {
                throw badRequest("File size must not exceed 5 MB");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void requireAuthenticated(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
    }

    // Browsers may send a full path; keep only a bounded file name for display.
    private static String displayName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw badRequest("File name is required");
        }
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (name.isEmpty()) {
            throw badRequest("File name is required");
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String extension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? "" : fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
