package com.qrfileshare.service;

import com.qrfileshare.model.FileShare;
import com.qrfileshare.repository.FileShareRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileShareService {

    private final FileShareRepository repository;
    private final FileStorageService storageService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.default-expiry-hours}")
    private int defaultExpiryHours;

    public FileShareService(FileShareRepository repository,
                            FileStorageService storageService,
                            BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Handle the full upload flow:
     * 1. Generate UUID
     * 2. Upload file to Supabase Storage
     * 3. Save metadata to DB
     */
    public FileShare uploadFile(MultipartFile file, String password, int expiryHours) throws IOException {
        UUID fileId = UUID.randomUUID();

        // Create storage path: files/<uuid>_<original-filename>
        String safeFileName = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storagePath = "files/" + fileId + "_" + safeFileName;

        // Upload to Supabase Storage
        storageService.uploadFile(storagePath, file);

        // Build the DB record
        FileShare fileShare = new FileShare();
        fileShare.setId(fileId);
        fileShare.setFileName(file.getOriginalFilename());
        fileShare.setStoragePath(storagePath);
        fileShare.setExpiryTime(LocalDateTime.now().plusHours(expiryHours > 0 ? expiryHours : defaultExpiryHours));

        // Hash password if provided
        if (password != null && !password.isBlank()) {
            fileShare.setPasswordHash(passwordEncoder.encode(password));
        }

        return repository.save(fileShare);
    }

    public Optional<FileShare> findById(UUID fileId) {
        return repository.findById(fileId);
    }

    public boolean verifyPassword(FileShare fileShare, String rawPassword) {
        return passwordEncoder.matches(rawPassword, fileShare.getPasswordHash());
    }

    public String generateSignedUrl(FileShare fileShare) {
        return storageService.createSignedUrl(fileShare.getStoragePath(), 60*3); // Default expiry of signed URI
    }

    public void incrementDownloadCount(FileShare fileShare) {
        fileShare.setDownloadCount(fileShare.getDownloadCount() + 1);
        repository.save(fileShare);
    }
}

