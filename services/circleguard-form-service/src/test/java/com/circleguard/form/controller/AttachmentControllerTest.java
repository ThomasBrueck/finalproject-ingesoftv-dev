package com.circleguard.form.controller;

import com.circleguard.form.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttachmentController.class)
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    void shouldUploadAttachment() throws Exception {
        when(storageService.store(any())).thenReturn("uploaded-file.pdf");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/attachments").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("uploaded-file.pdf"));
    }

    @Test
    void shouldFailWithInvalidFile() throws Exception {
        when(storageService.store(any())).thenThrow(new RuntimeException("Invalid file"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/attachments").file(file))
                .andExpect(status().is(500));
    }
}
