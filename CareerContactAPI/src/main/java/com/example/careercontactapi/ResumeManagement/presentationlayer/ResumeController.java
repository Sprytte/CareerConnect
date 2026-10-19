package com.example.careercontactapi.ResumeManagement.presentationlayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<ResumeResponseModel> uploadResume(
            @PathVariable String userId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        return ResponseEntity.status(201)
                .body(resumeService.uploadResumeForUser(
                        userId,
                        authenticatedUserId(authentication),
                        file
                ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ResumeResponseModel>> getUserResumes(
            @PathVariable String userId,
            Authentication authentication) {

        return ResponseEntity.ok(resumeService.getResumesForUser(
                userId,
                authenticatedUserId(authentication)
        ));
    }

    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable Integer resumeId,
            Authentication authentication) {

        ResumeFile resumeFile = resumeService.downloadResumeForUser(
                resumeId,
                authenticatedUserId(authentication)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(resumeFile.originalName(), StandardCharsets.UTF_8)
                        .build()
        );
        headers.setContentType(
                MediaType.parseMediaType(resumeFile.contentType())
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(resumeFile.resource());
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Integer resumeId,
            Authentication authentication) {

        resumeService.deleteResumeForUser(
                resumeId,
                authenticatedUserId(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return authentication.getName();
    }
}