package com.circleguard.auth.controller;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocalUserRepository localUserRepository;

    @Test
    void shouldReturnUsersByPermission() throws Exception {
        LocalUser user = LocalUser.builder()
                .username("admin1")
                .email("admin@university.edu")
                .build();

        when(localUserRepository.findUsersByPermissionName("alert:receive_priority"))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users/permissions/alert:receive_priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin1"))
                .andExpect(jsonPath("$[0].email").value("admin@university.edu"));

        verify(localUserRepository).findUsersByPermissionName("alert:receive_priority");
    }

    @Test
    void shouldReturnEmptyListWhenNoUsersHavePermission() throws Exception {
        when(localUserRepository.findUsersByPermissionName("nonexistent"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/permissions/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldHandleNullEmailGracefully() throws Exception {
        LocalUser user = LocalUser.builder()
                .username("nobody")
                .email(null)
                .build();

        when(localUserRepository.findUsersByPermissionName("test")).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users/permissions/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(""));
    }
}
