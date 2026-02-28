package com.qrfileshare.controller;

import com.qrfileshare.model.FileShare;
import com.qrfileshare.service.DownloadTokenStore;
import com.qrfileshare.service.FileShareService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Controller
public class DownloadController {

    private final FileShareService fileShareService;
    private final DownloadTokenStore tokenStore;

    public DownloadController(FileShareService fileShareService, DownloadTokenStore tokenStore) {
        this.fileShareService = fileShareService;
        this.tokenStore = tokenStore;
    }

    // Step 1: Show page (password form / expired / direct stream trigger)
    @GetMapping("/download/{fileId}")
    public String download(@PathVariable String fileId, Model model) {
        Optional<FileShare> optionalFile = findValidFile(fileId, model);
        if (optionalFile.isEmpty()) return "expired";

        FileShare fileShare = optionalFile.get();

        if (fileShare.isPasswordProtected()) {
            model.addAttribute("fileId", fileId);
            model.addAttribute("fileName", fileShare.getFileName());
            return "download"; // ← shows password form
        }

        // No password: show a page that auto-triggers /stream
        model.addAttribute("fileId", fileId);
        model.addAttribute("fileName", fileShare.getFileName());
        return "start-download"; // ← auto-download page (new template)
    }

    // Step 2: Stream the file through Spring Boot
    @GetMapping("/download/{fileId}/stream")
    public void stream(@PathVariable String fileId,
                       @RequestParam(required = false) String token,
                       HttpServletResponse response) throws IOException {

        UUID uuid;
        try { uuid = UUID.fromString(fileId); }
        catch (IllegalArgumentException e) { response.sendError(404); return; }

        Optional<FileShare> opt = fileShareService.findById(uuid);
        if (opt.isEmpty()) { response.sendError(404); return; }

        FileShare fileShare = opt.get();

        if (fileShare.isExpired()) { response.sendError(410, "File expired"); return; }

        // Password-protected: must have a valid one-time token
        if (fileShare.isPasswordProtected()) {
            if (token == null || tokenStore.validateAndConsume(token, fileId) == null) {
                response.sendError(403, "Invalid or expired token");
                return;
            }
        }

        fileShareService.streamFile(fileShare, response);
        fileShareService.incrementDownloadCount(fileShare);
    }

    // Helper
    private Optional<FileShare> findValidFile(String fileId, Model model) {
        UUID uuid;
        try { uuid = UUID.fromString(fileId); }
        catch (IllegalArgumentException e) {
            model.addAttribute("message", "Invalid file link.");
            return null;
        }
        Optional<FileShare> opt = fileShareService.findById(uuid);
        if (opt.isEmpty()) {
            model.addAttribute("message", "File not found or has been deleted.");
            return null;
        }
        if (opt.get().isExpired()) {
            model.addAttribute("message", "This file has expired and is no longer available.");
            return null;
        }
        return opt;
    }
}
