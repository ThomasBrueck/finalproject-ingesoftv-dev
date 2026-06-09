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
    void shouldSaveFileWithGeneratedName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        String filename = fileStorageService.saveFile(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith("_test.pdf"));
        assertTrue(Files.exists(Paths.get("uploads", filename)));
    }

    @Test
    void shouldCreateUploadsDirectoryOnInit() {
        assertTrue(Files.exists(Paths.get("uploads")));
    }
}
