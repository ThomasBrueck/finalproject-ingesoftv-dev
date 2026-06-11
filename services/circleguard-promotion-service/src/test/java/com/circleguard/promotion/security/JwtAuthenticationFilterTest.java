package com.circleguard.promotion.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Key;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "my-very-long-secret-key-that-is-at-least-256-bits-long-for-hs256";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidJwt_ShouldSetAuthentication() throws Exception {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .setSubject("user-1")
                .claim("permissions", List.of("ROLE_USER"))
                .signWith(key)
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(auth);
        assertEquals("user-1", auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void doFilterInternal_WithInvalidJwt_ShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithNoAuthHeader_ShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithNonBearerHeader_ShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic somebase64==");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithValidJwtAndPermissions_ShouldMapAuthorities() throws Exception {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        List<String> permissions = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = Jwts.builder()
                .setSubject("admin-1")
                .claim("permissions", permissions)
                .signWith(key)
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(auth);
        assertEquals("admin-1", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
    }
}
