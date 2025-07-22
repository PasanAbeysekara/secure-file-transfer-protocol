package com.example.securetransfer.dto;

import java.util.UUID;

public class TransferResponse {
    private UUID transferId;
    private String message;

    public TransferResponse(UUID transferId, String message) {
        this.transferId = transferId;
        this.message = message;
    }

    // Getters and Setters
    public UUID getTransferId() { return transferId; }
    public void setTransferId(UUID transferId) { this.transferId = transferId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}