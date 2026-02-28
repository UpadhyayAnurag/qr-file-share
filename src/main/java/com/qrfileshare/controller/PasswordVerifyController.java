package com. qrfileshare.controller;

import com. qrfileshare.model.FileShare;
import com. qrfileshare.service.DownloadTokenStore;
import com. qrfileshare.service.FileShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PasswordVerifyController {

    private final FileShareService fileShareService;
    private final DownloadTokenStore tokenStore;

    public PasswordVerifyController(FileShareService fileShareService, DownloadTokenStore tokenStore) {
        this.fileShareService = fileShareService;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String fileId  = body.get("fileId");
        String password = body.get("password");

        if (fileId == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "fileId and password required"));

        Optional<FileShare> opt;
        try { opt = fileShareService.findById(UUID.fromString(fileId)); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file ID"));
        }

        if (opt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("error", "File not found"));

        FileShare fileShare = opt.get();

        if (fileShare.isExpired())
            return ResponseEntity.status(410).body(Map.of("error", "File has expired"));

        if (!fileShareService.verifyPassword(fileShare, password))
            return ResponseEntity.status(401).body(Map.of("error", "Incorrect password"));

        // Issue a one-time 60-second stream token
        String streamToken = tokenStore.issue(fileId);
        return ResponseEntity.ok(Map.of("streamToken", streamToken));
    }
}
