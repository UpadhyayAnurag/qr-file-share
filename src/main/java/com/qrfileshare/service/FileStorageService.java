package com.qrfileshare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final RestTemplate restTemplate;

    // Constructor Injection
    public FileStorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Upload a file to Supabase Storage.
     * @param storagePath  Path inside the bucket, e.g. "files/uuid_filename.pdf"
     * @param file         The multipart file from the HTTP request
     * @return             The storagePath on success
     */
    public String uploadFile(String storagePath, MultipartFile file) throws IOException {
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase upload failed: " + response.getBody());
        }

        log.info("Uploaded file to Supabase: {}", storagePath);
        return storagePath;
    }

    /**
     * Generate a short-lived signed URL for downloading a file.
     * @param storagePath  Path inside the bucket
     * @param expirySeconds  How long the URL is valid (we use 60 seconds)
     * @return             Signed URL string
     */
    public String createSignedUrl(String storagePath, int expirySeconds) {
        String url = supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + storagePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("expiresIn", expirySeconds);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to create signed URL");
        }

        String signedPath = (String) response.getBody().get("signedURL");
        return supabaseUrl + signedPath;
    }

    /**
     * Delete a file from Supabase Storage.
     * @param storagePath  Path inside the bucket
     */
    public void deleteFile(String storagePath) {
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            log.info("Deleted file from Supabase: {}", storagePath);
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase: {}", storagePath, e);
        }
    }
}

