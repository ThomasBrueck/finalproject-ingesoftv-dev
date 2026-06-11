package com.circleguard.file.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path root = Paths.get("uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String saveFile(MultipartFile file) {
        // Reject names that attempt path traversal, then verify the resolved
        // path stays inside the storage root (S2083).
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
            if (!destination.startsWith(this.root.normalize())) {
                throw new RuntimeException("Cannot store file outside the storage directory");
            }
            Files.copy(file.getInputStream(), destination);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    public Resource loadFile(String filename) {
        // Implement retrieval logic
        return null; 
    }
}
interface Resource {}
