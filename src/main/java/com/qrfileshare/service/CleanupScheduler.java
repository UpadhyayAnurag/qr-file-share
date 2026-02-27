package com.qrfileshare.service;

import com.qrfileshare.model.FileShare;
import com.qrfileshare.repository.FileShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final FileShareRepository repository;
    private final FileStorageService storageService;

    public CleanupScheduler(FileShareRepository repository, FileStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    public void cleanupExpiredFiles() {
        List<FileShare> expired = repository.findByExpiryTimeBefore(LocalDateTime.now());

        if (expired.isEmpty()) {
            log.debug("Cleanup: no expired files found");
            return;
        }

        log.info("Cleanup: found {} expired file(s) to delete", expired.size());

        for (FileShare fileShare : expired) {
            try {
                // 1. Delete from Supabase Storage
                storageService.deleteFile(fileShare.getStoragePath());
                // 2. Delete metadata from DB
                repository.delete(fileShare);
                log.info("Deleted expired file: {}", fileShare.getFileName());
            } catch (Exception e) {
                log.error("Failed to delete expired file: {}", fileShare.getId(), e);
            }
        }
    }
}
