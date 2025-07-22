package com.example.securetransfer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    // PRODUCTION: Replace this service with an implementation that uses a cloud
    // object store like Amazon S3 or Azure Blob Storage.
    private final Path rootLocation;

    public FileStorageService(@Value("${file.storage.location}") String storageLocation) throws IOException {
        this.rootLocation = Paths.get(storageLocation);
        Files.createDirectories(rootLocation);
    }

    public String store(MultipartFile file) throws IOException {
        String storedFileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return storedFileName;
    }

    public Path storeDecrypted(byte[] decryptedData, String originalFileName) throws IOException {
        String storedFileName = "decrypted-" + UUID.randomUUID() + "-" + originalFileName;
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize().toAbsolutePath();
        Files.write(destinationFile, decryptedData);
        return destinationFile;
    }

    public Resource loadAsResource(String filename) throws MalformedURLException {
        Path file = rootLocation.resolve(filename);
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read file: " + filename);
        }
    }
    
    public byte[] loadAsBytes(String filename) throws IOException {
        Path file = rootLocation.resolve(filename);
        return Files.readAllBytes(file);
    }
}