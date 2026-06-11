package com.circleguard.form.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    @Test
    void shouldStoreFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        String filename = storageService.store(file);

        assertThat(filename).endsWith("_test.txt");
    }
}
