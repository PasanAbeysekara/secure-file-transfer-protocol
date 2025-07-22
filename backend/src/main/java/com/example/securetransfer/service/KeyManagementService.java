package com.example.securetransfer.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KeyManagementService {

    private final Map<String, KeyPair> userKeys = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        userKeys.put("alice", generateRsaKeyPair());
        userKeys.put("bob", generateRsaKeyPair());
        userKeys.put("charlie", generateRsaKeyPair());
    }

    private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public PublicKey getPublicKey(String username) {
        KeyPair keyPair = userKeys.get(username.toLowerCase());
        if (keyPair == null) throw new IllegalArgumentException("No keys found for user: " + username);
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey(String username) {
        KeyPair keyPair = userKeys.get(username.toLowerCase());
        if (keyPair == null) throw new IllegalArgumentException("No keys found for user: " + username);
        return keyPair.getPrivate();
    }
}