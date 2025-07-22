package com.example.securetransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SecureTransferProdApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureTransferProdApplication.class, args);
    }
}