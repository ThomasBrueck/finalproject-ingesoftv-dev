package com.circleguard.form.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    private final Path root;

    // Configurable storage location; defaults to an app-local "uploads" dir
    // instead of a world-writable /tmp path (S5443).
    public StorageService(@Value("${app.storage.dir:uploads}") String storageDir) {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String store(MultipartFile file) {
        // Reject names that attempt path traversal, then verify the resolved
        // path stays inside root to avoid path traversal (S2083).
        String original = file.getOriginalFilename();
        if (original != null
                && (original.contains("..") || original.contains("/") || original.contains("\\"))) {
            throw new RuntimeException("Invalid file name: " + original);
        }

        String safeName = (original == null) ? "file"
                : Paths.get(original).getFileName().toString();
        String filename = UUID.randomUUID() + "_" + safeName;
        try {
            Path destination = this.root.resolve(filename).normalize();
            if (!destination.startsWith(this.root)) {
                throw new RuntimeException("Cannot store file outside the storage directory");
            }
            Files.copy(file.getInputStream(), destination);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage(), e);
        }
    }
}
