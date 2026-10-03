package com.accordion.controller;

import com.accordion.model.Channel;
import com.accordion.model.ChatMessage;
import com.accordion.service.ChannelService;
import com.accordion.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Covers the STOMP send and join handlers. The typing handler is covered by
 * {@link TypingIndicatorTest}.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final Long DEFAULT_CHANNEL_ID = 1L;
    private static final Long OTHER_CHANNEL_ID = 7L;

    @Mock
    private ChatService chatService;

    @Mock
    private ChannelService channelService;

    @InjectMocks
    private ChatController chatController;

    private Channel defaultChannel;
    private Channel otherChannel;

    @BeforeEach
    void setUp() {
        // Same values as backend/src/main/resources/application.properties
        ReflectionTestUtils.setField(chatController, "maxMessageLength", 1000);
        ReflectionTestUtils.setField(chatController, "maxUsernameLength", 50);
        ReflectionTestUtils.setField(chatController, "minUsernameLength", 3);

        defaultChannel = new Channel("general", "General discussion", "System");
        defaultChannel.setId(DEFAULT_CHANNEL_ID);
        otherChannel = new Channel("random", "Random stuff", "alice");
        otherChannel.setId(OTHER_CHANNEL_ID);
    }

    private static Map<String, String> payload(String username, String content) {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("content", content);
        return payload;
    }

    // --- /chat.send ---

    @Test
    void testSendMessage_SavesTrimmedMessageToDefaultChannel() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        ChatMessage saved = new ChatMessage("alice", "hello", DEFAULT_CHANNEL_ID);
        when(chatService.saveMessage("alice", "hello", DEFAULT_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.sendMessage(payload("  alice  ", "  hello  "));

        assertSame(saved, result);
        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
    }

    @Test
    void testSendMessage_IgnoresChannelIdInPayload() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        Map<String, String> payload = payload("alice", "hello");
        payload.put("channelId", String.valueOf(OTHER_CHANNEL_ID));

        chatController.sendMessage(payload);

        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
        verify(channelService, never()).getChannelById(anyLong());
    }

    @Test
    void testSendMessage_NullPayload() {
        assertThrows(IllegalArgumentException.class, () -> chatController.sendMessage(null));
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_InvalidUsername() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("al", "hello")));

        assertEquals("Invalid username", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_UsernameWithIllegalCharacters() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("alice!", "hello")));

        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_WhitespaceOnlyContent() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("alice", "   ")));

        assertEquals("Invalid message content", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_ContentAtMaxLengthIsAccepted() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        String content = "a".repeat(1000);

        chatController.sendMessage(payload("alice", content));

        verify(chatService).saveMessage("alice", content, DEFAULT_CHANNEL_ID);
    }

    @Test
    void testSendMessage_ContentOverMaxLengthIsRejected() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("alice", "a".repeat(1001))));

        verifyNoInteractions(chatService);
    }

    // --- /chat.send/{channelId} ---

    @Test
    void testSendMessageToChannel_SavesTrimmedMessageToThatChannel() {
        when(channelService.getChannelById(OTHER_CHANNEL_ID)).thenReturn(Optional.of(otherChannel));
        ChatMessage saved = new ChatMessage("alice", "hello", OTHER_CHANNEL_ID);
        when(chatService.saveMessage("alice", "hello", OTHER_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.sendMessageToChannel(OTHER_CHANNEL_ID, payload(" alice ", " hello "));

        assertSame(saved, result);
        verify(channelService, never()).getOrCreateDefaultChannel();
    }

    @Test
    void testSendMessageToChannel_ChannelNotFound() {
        when(channelService.getChannelById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(999L, payload("alice", "hello")));

        assertEquals("Channel does not exist", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessageToChannel_NullPayload() {
        assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(OTHER_CHANNEL_ID, null));
        verifyNoInteractions(chatService, channelService);
    }

    @Test
    void testSendMessageToChannel_InvalidUsernameRejectedBeforeChannelLookup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(OTHER_CHANNEL_ID, payload(null, "hello")));

        assertEquals("Invalid username", ex.getMessage());
        verifyNoInteractions(chatService, channelService);
    }

    @Test
    void testSendMessageToChannel_MissingContentRejectedBeforeChannelLookup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(OTHER_CHANNEL_ID, payload("alice", null)));

        assertEquals("Invalid message content", ex.getMessage());
        verifyNoInteractions(chatService, channelService);
    }

    // --- /chat.join ---

    @Test
    void testUserJoin_SavesSystemMessageToDefaultChannel() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        ChatMessage saved = new ChatMessage("System", "alice has joined the chat", DEFAULT_CHANNEL_ID);
        when(chatService.saveMessage("System", "alice has joined the chat", DEFAULT_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.userJoin(payload("  alice  ", null));

        assertSame(saved, result);
    }

    @Test
    void testUserJoin_NullPayload() {
        assertThrows(IllegalArgumentException.class, () -> chatController.userJoin(null));
        verifyNoInteractions(chatService);
    }

    @Test
    void testUserJoin_InvalidUsername() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.userJoin(payload("a".repeat(51), null)));

        assertEquals("The 'username' field must be valid", ex.getMessage());
        verify(chatService, never()).saveMessage(anyString(), anyString(), any());
    }

    // --- /chat.join/{channelId} ---

    @Test
    void testUserJoinChannel_SavesSystemMessageToThatChannel() {
        when(channelService.getChannelById(OTHER_CHANNEL_ID)).thenReturn(Optional.of(otherChannel));
        ChatMessage saved = new ChatMessage("System", "alice has joined the chat", OTHER_CHANNEL_ID);
        when(chatService.saveMessage("System", "alice has joined the chat", OTHER_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.userJoinChannel(OTHER_CHANNEL_ID, payload(" alice ", null));

        assertSame(saved, result);
        verify(channelService, never()).getOrCreateDefaultChannel();
    }

    @Test
    void testUserJoinChannel_ChannelNotFound() {
        when(channelService.getChannelById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.userJoinChannel(999L, payload("alice", null)));

        assertEquals("Channel does not exist", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testUserJoinChannel_NullPayload() {
        assertThrows(IllegalArgumentException.class,
            () -> chatController.userJoinChannel(OTHER_CHANNEL_ID, null));
        verifyNoInteractions(chatService, channelService);
    }

    @Test
    void testUserJoinChannel_InvalidUsernameRejectedBeforeChannelLookup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.userJoinChannel(OTHER_CHANNEL_ID, payload("   ", null)));

        assertEquals("The 'username' field must be valid", ex.getMessage());
        verifyNoInteractions(chatService, channelService);
    }
}
