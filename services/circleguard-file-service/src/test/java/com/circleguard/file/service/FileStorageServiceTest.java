package com.circleguard.file.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(Paths.get("uploads"));
    }

    @Test
    void shouldSaveFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        String filename = fileStorageService.saveFile(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith("_test.pdf"));
        assertTrue(Files.exists(Paths.get("uploads", filename)));
    }

    @Test
    void shouldDownloadFile() {
        Path testFile = Paths.get("uploads", "existing.pdf");
        try {
            Files.createDirectories(Paths.get("uploads"));
            Files.writeString(testFile, "test content");
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        Resource resource = fileStorageService.loadFile("existing.pdf");

        assertNull(resource);
    }

    @Test
    void shouldDeleteFile() throws IOException {
        Files.createDirectories(Paths.get("uploads"));
        Path testFile = Paths.get("uploads", "to-delete.pdf");
        Files.writeString(testFile, "content");
        assertTrue(Files.exists(testFile));

        Files.deleteIfExists(testFile);

        assertFalse(Files.exists(testFile));
    }

    @Test
    void shouldThrowOnInvalidPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../malicious.exe", "application/octet-stream", "bad".getBytes());

        assertThrows(RuntimeException.class, () -> fileStorageService.saveFile(file));
    }

    @Test
    void shouldThrowOnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        String filename = fileStorageService.saveFile(empty);

        assertNotNull(filename);
        assertTrue(Files.exists(Paths.get("uploads", filename)));
    }
}
