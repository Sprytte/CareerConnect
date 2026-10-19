package com.example.careercontactapi.ResumeManagement.presentationlayer;

import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

    @PostMapping
    public ResponseEntity<ResumeResponseModel> uploadResume(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.uploadResumeForUser(
                        authenticatedUserId(principal),
                        file
                ));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponseModel>> getUserResumes(
            @AuthenticationPrincipal OidcUser principal) {

        return ResponseEntity.ok(resumeService.getResumesForUser(
                authenticatedUserId(principal)
        ));
    }

    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable Integer resumeId,
            @AuthenticationPrincipal OidcUser principal) {

        ResumeFile resumeFile = resumeService.downloadResumeForUser(
                resumeId,
                authenticatedUserId(principal)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(resumeFile.originalName(), StandardCharsets.UTF_8)
                        .build()
        );
        headers.setContentType(MediaType.parseMediaType(resumeFile.contentType()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resumeFile.resource());
    }

    @PutMapping("/{resumeId}")
    public ResponseEntity<ResumeResponseModel> updateResume(
            @PathVariable Integer resumeId,
            @RequestBody String filename,
            @AuthenticationPrincipal OidcUser principal) {

        return ResponseEntity.ok(resumeService.updateResume(
                resumeId,
                filename,
                authenticatedUserId(principal)
        ));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Integer resumeId,
            @AuthenticationPrincipal OidcUser principal) {

        resumeService.deleteResumeForUser(
                resumeId,
                authenticatedUserId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private String authenticatedUserId(OidcUser principal) {
        if (principal == null
                || principal.getSubject() == null
                || principal.getSubject().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return principal.getSubject();
    }
}
