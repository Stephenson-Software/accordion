package com.accordion.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest requestWith(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/channels");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }

    private static UserDetails userDetails(String username) {
        return new User(username, "encoded-password", Collections.emptyList());
    }

    /** Runs the filter and asserts the request always continues down the chain. */
    private void runFilter(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertSame(request, chain.getRequest());
    }

    @Test
    void testDoFilter_NoAuthorizationHeaderLeavesRequestUnauthenticated() throws Exception {
        runFilter(requestWith(null));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testDoFilter_NonBearerAuthorizationHeaderIgnored() throws Exception {
        runFilter(requestWith("Basic YWxpY2U6c2VjcmV0"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testDoFilter_ValidTokenAuthenticatesUser() throws Exception {
        UserDetails alice = userDetails("alice");
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(alice);
        when(jwtUtil.validateToken(TOKEN, "alice")).thenReturn(true);

        runFilter(requestWith("Bearer " + TOKEN));

        UsernamePasswordAuthenticationToken authentication = assertInstanceOf(
            UsernamePasswordAuthenticationToken.class,
            SecurityContextHolder.getContext().getAuthentication());
        assertSame(alice, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertTrue(authentication.isAuthenticated());
        assertEquals("alice", authentication.getName());
    }

    @Test
    void testDoFilter_TokenFailingValidationLeavesRequestUnauthenticated() throws Exception {
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails("alice"));
        when(jwtUtil.validateToken(TOKEN, "alice")).thenReturn(false);

        runFilter(requestWith("Bearer " + TOKEN));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilter_UnparseableTokenLeavesRequestUnauthenticated() throws Exception {
        when(jwtUtil.extractUsername(TOKEN)).thenThrow(new IllegalArgumentException("malformed token"));

        runFilter(requestWith("Bearer " + TOKEN));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testDoFilter_UnknownUserLeavesRequestUnauthenticated() throws Exception {
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
            .thenThrow(new UsernameNotFoundException("User not found: ghost"));

        runFilter(requestWith("Bearer " + TOKEN));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }

    @Test
    void testDoFilter_ExistingAuthenticationNotReplaced() throws Exception {
        Authentication existing = new TestingAuthenticationToken("bob", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("alice");

        runFilter(requestWith("Bearer " + TOKEN));

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }
}
