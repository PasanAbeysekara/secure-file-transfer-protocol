package com.example.securetransfer.service;

import com.example.securetransfer.dto.TransferSummary;
import com.example.securetransfer.util.CryptoUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SecureTransferService {

    private final KeyManagementService keyManagementService;

    public SecureTransferService(KeyManagementService keyManagementService) {
        this.keyManagementService = keyManagementService;
    }

    public TransferSummary performSecureTransfer(MultipartFile file, String sender, String receiver) throws Exception {
        Map<String, String> steps = new LinkedHashMap<>();

        // === KEY RETRIEVAL ===
        PublicKey senderPublicKey = keyManagementService.getPublicKey(sender);
        PrivateKey senderPrivateKey = keyManagementService.getPrivateKey(sender);
        PublicKey receiverPublicKey = keyManagementService.getPublicKey(receiver);
        PrivateKey receiverPrivateKey = keyManagementService.getPrivateKey(receiver);
        steps.put("Step 1", "RSA key pairs loaded for " + sender + " and " + receiver + ".");

        // === PHASE 1: HANDSHAKE (Sender -> Receiver) ===
        // 2. Sender (Alice) creates and signs a nonce
        String nonce = "nonce-" + System.currentTimeMillis();
        byte[] signedNonce = CryptoUtils.signData(nonce.getBytes(StandardCharsets.UTF_8), senderPrivateKey);
        byte[] encryptedNonce = CryptoUtils.rsaEncrypt(nonce.getBytes(StandardCharsets.UTF_8), receiverPublicKey);
        steps.put("Step 2", "Sender created, signed, and encrypted a nonce.");

        // TRANSMISSION SIMULATION
        // 3. Receiver (Bob) decrypts and verifies the nonce
        byte[] decryptedNonce = CryptoUtils.rsaDecrypt(encryptedNonce, receiverPrivateKey);
        boolean isSenderValid = CryptoUtils.verifySignature(decryptedNonce, signedNonce, senderPublicKey);
        steps.put("Step 3", "Receiver verified sender's nonce signature: " + isSenderValid);
        if (!isSenderValid) throw new SecurityException("Handshake failed: Invalid sender signature.");

        // === PHASE 2: AES KEY EXCHANGE (Receiver -> Sender) ===
        // 4. Receiver (Bob) generates AES session key and IV
        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256); // Using AES-256 for better security
        SecretKey aesSessionKey = aesGen.generateKey();
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        steps.put("Step 4", "Receiver generated AES-256 session key and IV.");

        // 5. Receiver (Bob) encrypts and signs the AES key/IV for the sender
        byte[] encryptedKey = CryptoUtils.rsaEncrypt(aesSessionKey.getEncoded(), senderPublicKey);
        byte[] encryptedIv = CryptoUtils.rsaEncrypt(iv, senderPublicKey);
        byte[] signedKey = CryptoUtils.signData(aesSessionKey.getEncoded(), receiverPrivateKey);
        byte[] signedIv = CryptoUtils.signData(iv, receiverPrivateKey);
        steps.put("Step 5", "Receiver encrypted and signed the session key/IV.");

        // TRANSMISSION SIMULATION
        // 6. Sender (Alice) decrypts and verifies the session key/IV
        byte[] decryptedKeyBytes = CryptoUtils.rsaDecrypt(encryptedKey, senderPrivateKey);
        byte[] decryptedIvBytes = CryptoUtils.rsaDecrypt(encryptedIv, senderPrivateKey);
        boolean isKeyValid = CryptoUtils.verifySignature(decryptedKeyBytes, signedKey, receiverPublicKey);
        boolean isIvValid = CryptoUtils.verifySignature(decryptedIvBytes, signedIv, receiverPublicKey);
        steps.put("Step 6", "Sender verified session key signature: " + isKeyValid + ", and IV signature: " + isIvValid);
        if (!isKeyValid || !isIvValid) throw new SecurityException("AES key exchange failed: Invalid signature from receiver.");

        SecretKey finalAesKey = new SecretKeySpec(decryptedKeyBytes, "AES");
        IvParameterSpec finalIvSpec = new IvParameterSpec(decryptedIvBytes);

        // === PHASE 3: FILE TRANSFER (Sender -> Receiver) ===
        // 7. Prepare file data
        byte[] fileData = file.getBytes();
        steps.put("Step 7", "Original file content loaded (" + fileData.length + " bytes).");

        // 8. Sender calculates file hash
        byte[] fileHash = CryptoUtils.calculateSHA256Hash(fileData);
        steps.put("Step 8", "Sender calculated file hash: " + CryptoUtils.bytesToHex(fileHash));
        
        // 9. Sender encrypts file using AES
        byte[] encryptedFile = CryptoUtils.aesEncrypt(fileData, finalAesKey, finalIvSpec);
        steps.put("Step 9", "Sender encrypted file with AES session key.");

        // 10. Sender signs the file hash
        byte[] signedFileHash = CryptoUtils.signData(fileHash, senderPrivateKey);
        steps.put("Step 10", "Sender signed the file hash.");

        // TRANSMISSION SIMULATION
        // 12. Receiver receives and verifies the signature on the hash
        boolean isFileHashSignatureValid = CryptoUtils.verifySignature(fileHash, signedFileHash, senderPublicKey);
        steps.put("Step 12", "Receiver verified sender's file hash signature: " + isFileHashSignatureValid);
        if (!isFileHashSignatureValid) throw new SecurityException("File transfer failed: Invalid file hash signature.");

        // 14. Receiver decrypts the file
        byte[] decryptedFileData = CryptoUtils.aesDecrypt(encryptedFile, finalAesKey, finalIvSpec);
        steps.put("Step 14", "Receiver decrypted the file with AES session key.");

        // 15. Receiver calculates hash of the decrypted file
        byte[] receiverCalculatedHash = CryptoUtils.calculateSHA256Hash(decryptedFileData);
        steps.put("Step 15", "Receiver calculated hash of decrypted file: " + CryptoUtils.bytesToHex(receiverCalculatedHash));

        // 16. Receiver compares the hashes for integrity check
        boolean hashesMatch = Arrays.equals(fileHash, receiverCalculatedHash);
        steps.put("Step 16", "Integrity check - Hashes match: " + hashesMatch);
        if (!hashesMatch) throw new SecurityException("File integrity check failed: Hashes do not match.");
        
        String decryptedContent = new String(decryptedFileData, StandardCharsets.UTF_8);
        
        return TransferSummary.builder()
            .message("Secure file transfer completed successfully!")
            .protocolSteps(steps)
            .finalDecryptedContent(decryptedContent)
            .build();
    }
}