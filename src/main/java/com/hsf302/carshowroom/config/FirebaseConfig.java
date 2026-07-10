package com.hsf302.carshowroom.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {
    private final String serviceAccountPath;
    private final String storageBucket;

    public FirebaseConfig(
            @Value("${firebase.service-account-path:}") String serviceAccountPath,
            @Value("${firebase.storage-bucket:}") String storageBucket) {
        this.serviceAccountPath = serviceAccountPath;
        this.storageBucket = storageBucket;
    }

    @PostConstruct
    void initialize() throws IOException {
        if (serviceAccountPath.isBlank() || storageBucket.isBlank() || !FirebaseApp.getApps().isEmpty()) {
            return;
        }
        Path credentialsPath = Path.of(serviceAccountPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(credentialsPath)) {
            throw new IllegalStateException("Firebase service account file not found: " + credentialsPath);
        }
        try (InputStream credentials = Files.newInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .setStorageBucket(storageBucket)
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
