package com.circleguard.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DualChainAuthenticationProviderTest {

    @Mock
    private LdapAuthenticationProvider ldapProvider;

    @Mock
    private DaoAuthenticationProvider localProvider;

    @InjectMocks
    private DualChainAuthenticationProvider provider;

    @Test
    void shouldAuthenticateViaLdapFirst() {
        Authentication input = new UsernamePasswordAuthenticationToken("user", "pass");
        Authentication expected = mock(Authentication.class);

        when(ldapProvider.authenticate(input)).thenReturn(expected);

        Authentication result = provider.authenticate(input);

        assertSame(expected, result);
        verify(localProvider, never()).authenticate(any());
    }

    @Test
    void shouldFallbackToLocalWhenLdapFails() {
        Authentication input = new UsernamePasswordAuthenticationToken("user", "pass");
        Authentication expected = mock(Authentication.class);

        when(ldapProvider.authenticate(input)).thenThrow(new BadCredentialsException("LDAP failed"));
        when(localProvider.authenticate(input)).thenReturn(expected);

        Authentication result = provider.authenticate(input);

        assertSame(expected, result);
    }

    @Test
    void shouldSupportUsernamePasswordToken() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldNotSupportOtherTokenTypes() {
        assertFalse(provider.supports(String.class));
    }
}
