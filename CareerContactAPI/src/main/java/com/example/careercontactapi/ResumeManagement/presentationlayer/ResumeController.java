package com.example.careercontactapi.ResumeManagement.presentationlayer;

import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeFile;
import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(201)
                .body(resumeService.uploadResume(userId, file));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ResumeResponseModel>> getUserResumes(
            @PathVariable String userId) {

        return ResponseEntity.ok(resumeService.getResumesByUser(userId));
    }

    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable Integer resumeId) {

        ResumeFile resumeFile = resumeService.downloadResume(resumeId);

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
            @PathVariable Integer resumeId) {

        resumeService.deleteResume(resumeId);

        return ResponseEntity.noContent().build();
    }
}