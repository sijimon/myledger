package com.myledger.controller;

import com.myledger.dto.FileResponse;
import com.myledger.entity.StoredFile;
import com.myledger.repository.StoredFileRepository;
import com.myledger.security.AppPrincipal;
import com.myledger.service.FileStorageService;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

/**
 * Upload and retrieval of receipts. Both are owner-only in M2 (SecurityConfig);
 * retrieval streams bytes through this authenticated endpoint rather than exposing a
 * public URL.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storage;
    private final StoredFileRepository files;

    public FileController(FileStorageService storage, StoredFileRepository files) {
        this.storage = storage;
        this.files = files;
    }

    @PostMapping("/upload")
    public FileResponse upload(@RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal AppPrincipal principal) {
        return FileResponse.from(storage.store(file, principal.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        StoredFile meta = files.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        Path path = storage.pathFor(meta);
        Resource resource = new PathResource(path);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(meta.getOriginalName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .contentLength(meta.getSizeBytes())
                .body(resource);
    }
}
