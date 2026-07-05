package com.myledger.service;

import com.myledger.config.StorageProperties;
import com.myledger.entity.StoredFile;
import com.myledger.repository.StoredFileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded receipts on local disk under opaque UUID keys and records metadata.
 * Bytes never sit in a web root; retrieval goes through an authenticated controller.
 */
@Service
@EnableConfigurationProperties(StorageProperties.class)
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "application/pdf");

    private final StoredFileRepository files;
    private final Path root;

    public FileStorageService(StoredFileRepository files, StorageProperties props) {
        this.files = files;
        this.root = Path.of(props.dir());
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + root, e);
        }
    }

    public StoredFile store(MultipartFile upload, Long uploaderUserId) {
        if (upload == null || upload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty upload");
        }
        String contentType = upload.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only images and PDF receipts are allowed");
        }

        String storageKey = UUID.randomUUID().toString();
        Path target = root.resolve(storageKey).normalize();
        // Defense in depth: the key is a UUID, but ensure we never escape the root.
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }

        try {
            Files.copy(upload.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        StoredFile meta = new StoredFile();
        meta.setStorageKey(storageKey);
        meta.setOriginalName(sanitizeName(upload.getOriginalFilename()));
        meta.setContentType(contentType);
        meta.setSizeBytes(upload.getSize());
        meta.setUploadedBy(uploaderUserId);
        return files.save(meta);
    }

    /** The on-disk path for a stored file, verified to exist. */
    public Path pathFor(StoredFile file) {
        Path path = root.resolve(file.getStorageKey()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return path;
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "receipt";
        }
        // Strip any path components a client might smuggle in.
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        return slash >= 0 ? base.substring(slash + 1) : base;
    }
}
