package com.accordion.controller;

import com.accordion.model.Channel;
import com.accordion.model.ChatMessage;
import com.accordion.security.CustomUserDetailsService;
import com.accordion.security.JwtUtil;
import com.accordion.service.ChannelService;
import com.accordion.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers what the unit tests cannot: that Spring really hands these handlers the authenticated
 * user. Each test runs through the configured STOMP or servlet pipeline with a forged author.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=TestSecretKeyForAuthorshipTestsMinimum32BytesRequired",
    "jwt.expiration=86400000",
    // Never report test runs to the trace service.
    "usage-reporting.enabled=false"
})
class AuthenticatedAuthorshipIntegrationTest {

    private static final long CHANNEL_ID = 7L;
    private static final long VERIFY_TIMEOUT_MILLIS = 5000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Qualifier("clientInboundChannel")
    private MessageChannel clientInboundChannel;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ChannelService channelService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private UserDetails alice;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "unused", List.of());
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(alice);

        Channel channel = new Channel("random", "Random stuff", "System");
        channel.setId(CHANNEL_ID);
        when(channelService.getChannelById(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(channelService.getOrCreateDefaultChannel()).thenReturn(channel);
        when(chatService.saveMessage("alice", "hello", CHANNEL_ID))
            .thenReturn(new ChatMessage("alice", "hello", CHANNEL_ID));
    }

    /** Delivers a SEND frame to the inbound channel as a session authenticated as alice. */
    private void sendAsAlice(String destination, String json) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setUser(new UsernamePasswordAuthenticationToken(alice, null, alice.getAuthorities()));

        clientInboundChannel.send(MessageBuilder.createMessage(
            json.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders()));
    }

    @Test
    void stompSend_ToDefaultChannel_IsAuthoredByTheSessionUser() {
        sendAsAlice("/app/chat.send", "{\"username\":\"mallory\",\"content\":\"hello\"}");

        verify(chatService, timeout(VERIFY_TIMEOUT_MILLIS)).saveMessage("alice", "hello", CHANNEL_ID);
    }

    @Test
    void stompSend_ToChannel_IsAuthoredByTheSessionUser() {
        sendAsAlice("/app/chat.send/" + CHANNEL_ID, "{\"username\":\"mallory\",\"content\":\"hello\"}");

        verify(chatService, timeout(VERIFY_TIMEOUT_MILLIS)).saveMessage("alice", "hello", CHANNEL_ID);
    }

    @Test
    void stompJoin_AnnouncesTheSessionUser() {
        sendAsAlice("/app/chat.join/" + CHANNEL_ID, "{\"username\":\"mallory\"}");

        verify(chatService, timeout(VERIFY_TIMEOUT_MILLIS))
            .saveMessage("System", "alice has joined the chat", CHANNEL_ID);
    }

    @Test
    void stompJoin_AcceptsAFrameWithNoBody() {
        sendAsAlice("/app/chat.join/" + CHANNEL_ID, "");

        verify(chatService, timeout(VERIFY_TIMEOUT_MILLIS))
            .saveMessage("System", "alice has joined the chat", CHANNEL_ID);
    }

    @Test
    void createChannel_RecordsTheTokenHolderAsCreator() throws Exception {
        when(channelService.createChannel("tech-talk", null, "alice"))
            .thenReturn(new Channel("tech-talk", null, "alice"));

        mockMvc.perform(post("/api/channels")
                .header("Authorization", "Bearer " + jwtUtil.generateToken("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"tech-talk\",\"createdBy\":\"mallory\"}"))
            .andExpect(status().isCreated());

        verify(channelService).createChannel("tech-talk", null, "alice");
    }
}
