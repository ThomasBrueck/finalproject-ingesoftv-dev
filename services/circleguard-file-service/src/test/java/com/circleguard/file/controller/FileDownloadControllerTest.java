package com.circleguard.file.controller;

import com.circleguard.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileDownloadController.class)
class FileDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService storageService;

    @Test
    void shouldDownloadFile() throws Exception {
        Resource resource = new ByteArrayResource("file content".getBytes());
        when(storageService.loadFile("test.pdf")).thenReturn(resource);

        mockMvc.perform(get("/api/v1/files/download/{filename}", "test.pdf"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenFileNotFound() throws Exception {
        when(storageService.loadFile("missing.pdf")).thenReturn(null);

        mockMvc.perform(get("/api/v1/files/download/{filename}", "missing.pdf"))
                .andExpect(status().isNotFound());
    }
}
