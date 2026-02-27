package com.qrfileshare.controller;

import com.qrfileshare.model.FileShare;
import com.qrfileshare.service.FileShareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;
import java.util.UUID;

@Controller
public class DownloadController {

    private final FileShareService fileShareService;

    public DownloadController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    @GetMapping("/download/{fileId}")
    public String download(@PathVariable String fileId, Model model) {
        UUID uuid;
        try {
            uuid = UUID.fromString(fileId);
        } catch (IllegalArgumentException e) {
            return "expired"; // Invalid ID -> show expired page
        }

        Optional<FileShare> optionalFile = fileShareService.findById(uuid);

        // File not found
        if (optionalFile.isEmpty()) {
            model.addAttribute("message", "File not found or has been deleted.");
            return "expired";
        }

        FileShare fileShare = optionalFile.get();

        // File expired
        if (fileShare.isExpired()) {
            model.addAttribute("message", "This file has expired and is no longer available.");
            return "expired";
        }

        // Password protected -> show password form
        if (fileShare.isPasswordProtected()) {
            model.addAttribute("fileId", fileId);
            model.addAttribute("fileName", fileShare.getFileName());
            return "download"; // -> download.html (shows a form)
        }

        // No password → generate signed URL and redirect
        String signedUrl = fileShareService.generateSignedUrl(fileShare);
        fileShareService.incrementDownloadCount(fileShare);
        return "redirect:" + signedUrl;
    }
}
