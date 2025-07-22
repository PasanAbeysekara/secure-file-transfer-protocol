package com.example.securetransfer.dto;

import com.example.securetransfer.model.TransferStatus;
import java.time.Instant;
import java.util.UUID;

public class TransferStatusResponse {
    private UUID id;
    private String sender;
    private String receiver;
    private String fileName;
    private TransferStatus status;
    private String failureReason;
    private Instant createdAt;
    
    // Private constructor for the builder
    private TransferStatusResponse(Builder builder) {
        this.id = builder.id;
        this.sender = builder.sender;
        this.receiver = builder.receiver;
        this.fileName = builder.fileName;
        this.status = builder.status;
        this.failureReason = builder.failureReason;
        this.createdAt = builder.createdAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public UUID getId() { return id; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getFileName() { return fileName; }
    public TransferStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }

    // Static Builder Class
    public static class Builder {
        private UUID id;
        private String sender;
        private String receiver;
        private String fileName;
        private TransferStatus status;
        private String failureReason;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sender(String sender) { this.sender = sender; return this; }
        public Builder receiver(String receiver) { this.receiver = receiver; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder status(TransferStatus status) { this.status = status; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        
        public TransferStatusResponse build() {
            return new TransferStatusResponse(this);
        }
    }
}