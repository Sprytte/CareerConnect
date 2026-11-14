package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer.ResumeFeedbackResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Validates an uploaded resume, prepares it for the AI service and returns
 * the generated feedback. The file is processed in memory and never stored.
 */
@Service
public class ResumeFeedbackService {

    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ResumeFeedbackGenerator generator;

    public ResumeFeedbackService(ResumeFeedbackGenerator generator) {
        this.generator = generator;
    }

    public ResumeFeedbackResponseModel requestFeedback(String authenticatedUserId, MultipartFile file) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return generator.generate(toDocument(file));
    }

    private ResumeDocument toDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("File size must not exceed 5 MB");
        }

        String fileName = displayName(file.getOriginalFilename());
        String extension = extension(fileName);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw badRequest("Only PDF and DOCX files are supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw badRequest("The uploaded file could not be read");
        }

        if (extension.equals("pdf")) {
            if (!startsWith(bytes, PDF_SIGNATURE)) {
                throw badRequest("The uploaded file is not a valid PDF");
            }
            return ResumeDocument.ofPdf(fileName, bytes);
        }

        String text;
        try {
            text = DocxTextExtractor.extract(bytes);
        } catch (IOException e) {
            throw badRequest("The uploaded file is not a valid DOCX document");
        }
        if (text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No readable text was found in the document"
            );
        }
        return ResumeDocument.ofText(fileName, text);
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
