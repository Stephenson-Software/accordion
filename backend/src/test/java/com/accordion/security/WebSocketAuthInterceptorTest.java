package com.accordion.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private static StompHeaderAccessor accessorFor(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        // Keep the headers mutable so preSend sees (and can set the user on) this same accessor
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> messageFor(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static UserDetails userDetails(String username) {
        return new User(username, "encoded-password", Collections.emptyList());
    }

    @Test
    void testPreSend_NonStompMessagePassesThrough() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        assertSame(message, interceptor.preSend(message, channel));
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testPreSend_NonConnectCommandPassesThroughWithoutAuthorization() {
        StompHeaderAccessor accessor = accessorFor(StompCommand.SEND, null);
        Message<byte[]> message = messageFor(accessor);

        assertSame(message, interceptor.preSend(message, channel));
        assertNull(accessor.getUser());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testPreSend_ConnectWithoutAuthorizationHeaderRejected() {
        Message<byte[]> message = messageFor(accessorFor(StompCommand.CONNECT, null));

        assertNull(interceptor.preSend(message, channel));
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testPreSend_ConnectWithNonBearerAuthorizationHeaderRejected() {
        Message<byte[]> message = messageFor(accessorFor(StompCommand.CONNECT, "Basic YWxpY2U6c2VjcmV0"));

        assertNull(interceptor.preSend(message, channel));
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void testPreSend_ConnectWithValidTokenAuthenticatesUser() {
        UserDetails alice = userDetails("alice");
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(alice);
        when(jwtUtil.validateToken(TOKEN, "alice")).thenReturn(true);
        StompHeaderAccessor accessor = accessorFor(StompCommand.CONNECT, "Bearer " + TOKEN);
        Message<byte[]> message = messageFor(accessor);

        assertSame(message, interceptor.preSend(message, channel));

        UsernamePasswordAuthenticationToken user = assertInstanceOf(
            UsernamePasswordAuthenticationToken.class, accessor.getUser());
        assertSame(alice, user.getPrincipal());
        assertTrue(user.isAuthenticated());
        assertEquals("alice", user.getName());
    }

    @Test
    void testPreSend_ConnectWithTokenFailingValidationRejected() {
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails("alice"));
        when(jwtUtil.validateToken(TOKEN, "alice")).thenReturn(false);
        StompHeaderAccessor accessor = accessorFor(StompCommand.CONNECT, "Bearer " + TOKEN);

        assertNull(interceptor.preSend(messageFor(accessor), channel));
        assertNull(accessor.getUser());
    }

    @Test
    void testPreSend_ConnectWithUnparseableTokenRejected() {
        when(jwtUtil.extractUsername(TOKEN)).thenThrow(new IllegalArgumentException("malformed token"));
        StompHeaderAccessor accessor = accessorFor(StompCommand.CONNECT, "Bearer " + TOKEN);

        assertNull(interceptor.preSend(messageFor(accessor), channel));
        assertNull(accessor.getUser());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_ConnectForUnknownUserRejected() {
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
            .thenThrow(new UsernameNotFoundException("User not found: ghost"));
        StompHeaderAccessor accessor = accessorFor(StompCommand.CONNECT, "Bearer " + TOKEN);

        assertNull(interceptor.preSend(messageFor(accessor), channel));
        assertNull(accessor.getUser());
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }
}
