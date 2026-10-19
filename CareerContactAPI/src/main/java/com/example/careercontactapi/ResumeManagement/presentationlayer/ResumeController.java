package com.example.careercontactapi.ResumeManagement.presentationlayer;

import com.example.careercontactapi.ResumeManagement.datalayer.ResumeFile;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @AuthenticationPrincipal OidcUser principal) {

        return ResponseEntity.status(201)
                .body(resumeService.uploadResumeForUser(
                        userId,
                        principal.getSubject(),
                        file
                ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ResumeResponseModel>> getUserResumes(
            @PathVariable String userId,
            @AuthenticationPrincipal OidcUser principal) {

        return ResponseEntity.ok(resumeService.getResumesForUser(
                userId,
                principal.getSubject()
        ));
    }

    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable Integer resumeId,
            @AuthenticationPrincipal OidcUser principal) {

        ResumeFile resumeFile = resumeService.downloadResumeForUser(
                resumeId,
                principal.getSubject()
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
            @AuthenticationPrincipal OidcUser principal) {

        resumeService.deleteResumeForUser(
                resumeId,
                principal.getSubject()
        );

        return ResponseEntity.noContent().build();
    }
}