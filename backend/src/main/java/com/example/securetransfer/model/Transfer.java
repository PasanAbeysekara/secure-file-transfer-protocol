package com.example.securetransfer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Transfer {
    @Id
    private UUID id;
    private String senderUsername;
    private String receiverUsername;
    private String originalFileName;
    private String storedFileName;
    private String decryptedFileName;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    private String failureReason;
    private Instant createdAt;
    private Instant completedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public String getDecryptedFileName() { return decryptedFileName; }
    public void setDecryptedFileName(String decryptedFileName) { this.decryptedFileName = decryptedFileName; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}