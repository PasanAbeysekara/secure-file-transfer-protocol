package com.example.securetransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSummary {
    private String message;
    private Map<String, String> protocolSteps;
    private String finalDecryptedContent;
}