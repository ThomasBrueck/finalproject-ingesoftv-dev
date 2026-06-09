package com.circleguard.auth.service;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.model.Permission;
import com.circleguard.auth.model.Role;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private LocalUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void shouldLoadActiveUserSuccessfully() {
        Permission read = Permission.builder().name("user:read").build();
        Role role = Role.builder().name("STUDENT").permissions(Set.of(read)).build();
        LocalUser user = LocalUser.builder()
                .username("testuser")
                .password("encoded-pass")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        assertEquals("testuser", result.getUsername());
        assertEquals("encoded-pass", result.getPassword());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("user:read")));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }

    @Test
    void shouldThrowDisabledExceptionWhenUserIsInactive() {
        LocalUser user = LocalUser.builder()
                .username("inactive")
                .password("pass")
                .isActive(false)
                .roles(Set.of())
                .build();

        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> service.loadUserByUsername("inactive"));
    }
}
