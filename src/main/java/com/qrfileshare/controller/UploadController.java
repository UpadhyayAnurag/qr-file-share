package com.qrfileshare.controller;

import com.qrfileshare.model.FileShare;
import com.qrfileshare.service.FileShareService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final FileShareService fileShareService;

    @Value("${app.base-url}")
    private String baseUrl;

    public UploadController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "expiryHours", defaultValue = "24") int expiryHours) {

        // Basic validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file selected"));
        }

        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large. Max 50MB allowed."));
        }

        try {
            FileShare saved = fileShareService.uploadFile(file, password, expiryHours);

            String downloadUrl = baseUrl + "/download/" + saved.getId();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("fileId", saved.getId().toString());
            response.put("downloadUrl", downloadUrl);
            response.put("expiresAt", saved.getExpiryTime().toString());
            response.put("passwordProtected", saved.isPasswordProtected());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
