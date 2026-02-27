package com.qrfileshare.repository;

import com.qrfileshare.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FileShareRepository extends JpaRepository<FileShare, UUID> {

    // Used by the cleanup scheduler: find all expired records
    List<FileShare> findByExpiryTimeBefore(LocalDateTime dateTime);
}
