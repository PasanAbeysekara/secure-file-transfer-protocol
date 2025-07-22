package com.example.securetransfer.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NonceService {
    // PRODUCTION: Replace this with a distributed cache like Redis.
    private final Map<String, Instant> nonceStore = new ConcurrentHashMap<>();
    private static final long NONCE_VALIDITY_SECONDS = 300; // 5 minutes

    public synchronized boolean isNonceValid(String nonce) {
        if (nonceStore.containsKey(nonce)) {
            return false; // Nonce has been used
        }
        nonceStore.put(nonce, Instant.now());
        return true;
    }

    // Clean up old nonces periodically to prevent memory leak
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanUpOldNonces() {
        Instant cutoff = Instant.now().minusSeconds(NONCE_VALIDITY_SECONDS);
        nonceStore.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}