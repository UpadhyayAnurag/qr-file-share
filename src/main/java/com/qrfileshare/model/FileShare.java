package com.qrfileshare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="file_share")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class FileShare {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "download_count")
    private int downloadCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Helpers
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryTime);
    }
    public boolean isPasswordProtected() {
        return this.passwordHash != null && !this.passwordHash.isEmpty();
    }
}
