package com.example.securetransfer.controller;

import com.example.securetransfer.dto.TransferSummary;
import com.example.securetransfer.service.SecureTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FileTransferController {

    private final SecureTransferService secureTransferService;

    public FileTransferController(SecureTransferService secureTransferService) {
        this.secureTransferService = secureTransferService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sender", defaultValue = "alice") String sender,
            @RequestParam(value = "receiver", defaultValue = "bob") String receiver) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File cannot be empty.");
            }
            TransferSummary summary = secureTransferService.performSecureTransfer(file, sender, receiver);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            // In a real app, log the exception and return a more generic error
            return ResponseEntity.status(500).body("An internal security or processing error occurred: " + e.getMessage());
        }
    }
}