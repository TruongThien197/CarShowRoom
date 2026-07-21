package com.hsf302.carshowroom.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FirebaseStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final String bucketName;

    public FirebaseStorageService(@Value("${firebase.storage-bucket:}") String bucketName) {
        this.bucketName = bucketName;
    }

    public String uploadProductImage(MultipartFile file) {
        validate(file);
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase Storage chưa được cấu hình.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String objectName = "products/" + UUID.randomUUID() + extension;
        String token = UUID.randomUUID().toString();
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .setMetadata(Map.of("firebaseStorageDownloadTokens", token))
                .build();
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            storage.create(blobInfo, file.getBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc tệp ảnh để tải lên Firebase.", exception);
        }

        String encodedName = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        return "https://firebasestorage.googleapis.com/v0/b/" + bucketName
                + "/o/" + encodedName + "?alt=media&token=" + token;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp ảnh không được để trống.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5 MB.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG, WebP hoặc GIF.");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
