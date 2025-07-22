package com.example.securetransfer.controller;

import com.example.securetransfer.dto.TransferResponse;
import com.example.securetransfer.dto.TransferStatusResponse;
import com.example.securetransfer.model.Transfer;
import com.example.securetransfer.model.TransferStatus;
import com.example.securetransfer.repository.TransferRepository;
import com.example.securetransfer.service.FileStorageService;
import com.example.securetransfer.service.SecureTransferProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final SecureTransferProtocolService protocolService;
    private final FileStorageService fileStorageService;
    private final TransferRepository transferRepository;

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @RequestParam("file") MultipartFile file,
            @RequestParam("receiver") String receiverUsername) throws IOException {

        String senderUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 1. Store the raw file temporarily
        String storedFileName = fileStorageService.store(file);

        // 2. Create a record in the database
        Transfer transfer = new Transfer();
        transfer.setId(UUID.randomUUID());
        transfer.setSenderUsername(senderUsername);
        transfer.setReceiverUsername(receiverUsername);
        transfer.setOriginalFileName(file.getOriginalFilename());
        transfer.setStoredFileName(storedFileName);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setCreatedAt(Instant.now());
        transferRepository.save(transfer);

        // 3. Trigger the async processing
        protocolService.processTransfer(transfer.getId(), senderUsername, receiverUsername);

        return new ResponseEntity<>(
            new TransferResponse(transfer.getId(), "Transfer initiated. Check status endpoint for progress."),
            HttpStatus.ACCEPTED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferStatusResponse> getTransferStatus(@PathVariable UUID id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Transfer> transferOptional = transferRepository.findById(id);

        if (transferOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Transfer transfer = transferOptional.get();

        // Authorization check: only sender or receiver can view status
        if (!transfer.getSenderUsername().equals(currentUsername) && !transfer.getReceiverUsername().equals(currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TransferStatusResponse response = TransferStatusResponse.builder()
            .id(transfer.getId())
            .sender(transfer.getSenderUsername())
            .receiver(transfer.getReceiverUsername())
            .fileName(transfer.getOriginalFileName())
            .status(transfer.getStatus())
            .failureReason(transfer.getFailureReason())
            .createdAt(transfer.getCreatedAt())
            .build();
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> downloadDecryptedFile(@PathVariable UUID id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Transfer> transferOptional = transferRepository.findById(id);

        if (transferOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Transfer transfer = transferOptional.get();

        // Authorization check: only receiver can download
        if (!transfer.getReceiverUsername().equals(currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Status check
        if (transfer.getStatus() != TransferStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict: The request could not be completed due to a conflict with the current state of the resource.
        }

        try {
            Resource resource = fileStorageService.loadAsResource(transfer.getDecryptedFileName());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + transfer.getOriginalFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            // Log the exception in a real app
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}