package com.example.careercontactapi.ResumeManagement.businesslayer;

import com.example.careercontactapi.ResumeManagement.datalayer.Resume;
import com.example.careercontactapi.ResumeManagement.datalayer.ResumeRepository;
import com.example.careercontactapi.ResumeManagement.presentationlayer.ResumeResponseModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final ResumeRepository resumeRepository;

    @Value("${resume.storage-path:uploads/resumes}")
    private String storagePath;

    public ResumeServiceImpl(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @Override
    public ResumeResponseModel uploadResume(String userId, MultipartFile file) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File name is required"
            );
        }

        String extension = getExtension(originalFileName);
        String contentType = getContentType(extension);
        String storedFileName = UUID.randomUUID() + "." + extension;

        try {
            Path directory = Paths.get(storagePath);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(storedFileName);
            file.transferTo(filePath);

            Resume resume = Resume.builder()
                    .userId(userId)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            return toResponseModel(resumeRepository.save(resume));

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store the uploaded file",
                    e
            );
        }
    }

    @Override
    public List<ResumeResponseModel> getResumesByUser(String userId) {
        return resumeRepository
                .findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::toResponseModel)
                .toList();
    }

    @Override
    public ResumeFile downloadResume(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        Path filePath = Paths.get(storagePath, resume.getStoredFileName());

        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resume file not found"
            );
        }

        return new ResumeFile(
                new FileSystemResource(filePath),
                resume.getOriginalFileName(),
                resume.getContentType()
        );
    }

    @Override
    public void deleteResume(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        Path filePath = Paths.get(storagePath, resume.getStoredFileName());

        try {
            Files.deleteIfExists(filePath);
            resumeRepository.delete(resume);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete resume",
                    e
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size must not exceed 5 MB"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File name is required"
            );
        }

        String extension = getExtension(fileName);

        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF and DOCX files are supported"
            );
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == -1) {
            return "";
        }

        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private String getContentType(String extension) {
        if (extension.equals("pdf")) {
            return "application/pdf";
        }

        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private ResumeResponseModel toResponseModel(Resume resume) {
        return new ResumeResponseModel(
                resume.getId(),
                resume.getUserId(),
                resume.getOriginalFileName(),
                resume.getContentType(),
                resume.getSizeBytes(),
                resume.getUploadedAt()
        );
    }
}