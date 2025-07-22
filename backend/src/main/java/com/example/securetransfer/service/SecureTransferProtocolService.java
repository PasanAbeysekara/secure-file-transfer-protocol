package com.example.securetransfer.service;

import com.example.securetransfer.model.Transfer;
import com.example.securetransfer.model.TransferStatus;
import com.example.securetransfer.repository.TransferRepository;
import com.example.securetransfer.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
// Add these two imports for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecureTransferProtocolService {

    // Manually define the logger field. This is what was missing.
    private static final Logger log = LoggerFactory.getLogger(SecureTransferProtocolService.class);

    private final TransferRepository transferRepository;
    private final KeyManagementService keyManagementService;
    private final NonceService nonceService;
    private final FileStorageService fileStorageService;

    @Async
    public void processTransfer(UUID transferId, String senderUsername, String receiverUsername) {
        log.info("Starting processing for transfer ID: {}", transferId);
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new IllegalStateException("Transfer not found for ID: " + transferId));

        try {
            // === KEY RETRIEVAL ===
            PublicKey senderPublicKey = keyManagementService.getPublicKey(senderUsername);
            PrivateKey senderPrivateKey = keyManagementService.getPrivateKey(senderUsername);
            PublicKey receiverPublicKey = keyManagementService.getPublicKey(receiverUsername);
            PrivateKey receiverPrivateKey = keyManagementService.getPrivateKey(receiverUsername);

            // === HANDSHAKE (Sender -> Receiver) ===
            String nonce = "nonce-" + UUID.randomUUID();
            byte[] signedNonce = CryptoUtils.signData(nonce.getBytes(StandardCharsets.UTF_8), senderPrivateKey);
            byte[] encryptedNonce = CryptoUtils.rsaEncrypt(nonce.getBytes(StandardCharsets.UTF_8), receiverPublicKey);

            // SIMULATED TRANSMISSION: Receiver decrypts
            byte[] decryptedNonce = CryptoUtils.rsaDecrypt(encryptedNonce, receiverPrivateKey);
            if (!nonceService.isNonceValid(new String(decryptedNonce, StandardCharsets.UTF_8))) {
                throw new SecurityException("Replay attack detected or invalid nonce.");
            }
            if (!CryptoUtils.verifySignature(decryptedNonce, signedNonce, senderPublicKey)) {
                throw new SecurityException("Handshake failed: Invalid sender signature.");
            }
            log.info("[{}] Handshake successful", transferId);

            // === AES KEY EXCHANGE (Receiver -> Sender) ===
            SecretKey aesSessionKey = new SecretKeySpec(new SecureRandom().generateSeed(32), "AES"); // AES-256
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            byte[] encryptedKey = CryptoUtils.rsaEncrypt(aesSessionKey.getEncoded(), senderPublicKey);
            byte[] signedKey = CryptoUtils.signData(aesSessionKey.getEncoded(), receiverPrivateKey);

            // SIMULATED TRANSMISSION: Sender decrypts
            byte[] decryptedKeyBytes = CryptoUtils.rsaDecrypt(encryptedKey, senderPrivateKey);
            if (!CryptoUtils.verifySignature(decryptedKeyBytes, signedKey, receiverPublicKey)) {
                throw new SecurityException("AES key exchange failed: Invalid signature from receiver.");
            }
            log.info("[{}] AES key exchange successful", transferId);
            
            SecretKey finalAesKey = new SecretKeySpec(decryptedKeyBytes, "AES");
            IvParameterSpec finalIvSpec = new IvParameterSpec(iv);

            // === FILE TRANSFER (Sender -> Receiver) ===
            byte[] fileData = fileStorageService.loadAsBytes(transfer.getStoredFileName());
            byte[] fileHash = CryptoUtils.calculateSHA256Hash(fileData);
            byte[] encryptedFile = CryptoUtils.aesEncrypt(fileData, finalAesKey, finalIvSpec);
            byte[] signedFileHash = CryptoUtils.signData(fileHash, senderPrivateKey);

            // SIMULATED TRANSMISSION: Receiver verifies and decrypts
            if (!CryptoUtils.verifySignature(fileHash, signedFileHash, senderPublicKey)) {
                throw new SecurityException("File transfer failed: Invalid file hash signature.");
            }
            byte[] decryptedFileData = CryptoUtils.aesDecrypt(encryptedFile, finalAesKey, finalIvSpec);

            byte[] receiverCalculatedHash = CryptoUtils.calculateSHA256Hash(decryptedFileData);
            if (!Arrays.equals(fileHash, receiverCalculatedHash)) {
                throw new SecurityException("File integrity check failed: Hashes do not match.");
            }
            log.info("[{}] File integrity check successful", transferId);

            // Store decrypted file
            Path decryptedFilePath = fileStorageService.storeDecrypted(decryptedFileData, transfer.getOriginalFileName());

            // Update transfer status to COMPLETED
            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(Instant.now());
            transfer.setDecryptedFileName(decryptedFilePath.getFileName().toString());
            transferRepository.save(transfer);
            log.info("Successfully completed transfer {}", transferId);

        } catch (Exception e) {
            log.error("Failed to process transfer {}: {}", transferId, e.getMessage(), e);
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailureReason(e.getClass().getSimpleName() + ": " + e.getMessage());
            transferRepository.save(transfer);
        }
    }
}