package com.qrfileshare.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DownloadTokenStore {

    record TokenEntry(String fileId, long expiresAt) {}

    private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

    // Issue a one-time token valid for 60 seconds
    public String issue(String fileId) {
        String token = UUID.randomUUID().toString();
        store.put(token, new TokenEntry(fileId, System.currentTimeMillis() + 60_000));
        return token;
    }

    // Validate token, consume it (one-time use), return fileId or null
    public String validateAndConsume(String token, String fileId) {
        TokenEntry entry = store.remove(token);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt()) return null;
        if (!entry.fileId().equals(fileId)) return null;
        return entry.fileId();
    }

    // Clean up expired tokens every minute
    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expiresAt());
    }
}
