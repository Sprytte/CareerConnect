package com.example.careercontactapi.ResumeManagement.presentationlayer;

import com.example.careercontactapi.ResumeManagement.businesslayer.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
//TODO the endpoint could probably be a part of /users ? TBD
@RequestMapping("api/v1/resumes")
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping("/users/{userId}/uploadImage")
    public ResponseEntity<String> addImageToUser(
            @PathVariable String userId,
            @RequestPart(value = "file") MultipartFile file) {

        return ResponseEntity.ok(resumeService.uploadUserImage(userId, file));
    }
}
