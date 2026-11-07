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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Covers the STOMP send and join handlers. The typing handler is covered by
 * {@link TypingIndicatorTest}.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final Long DEFAULT_CHANNEL_ID = 1L;
    private static final Long OTHER_CHANNEL_ID = 7L;
    private static final Principal ALICE = () -> "alice";

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
        // Same value as backend/src/main/resources/application.properties
        ReflectionTestUtils.setField(chatController, "maxMessageLength", 1000);

        defaultChannel = new Channel("general", "General discussion", "System");
        defaultChannel.setId(DEFAULT_CHANNEL_ID);
        otherChannel = new Channel("random", "Random stuff", "alice");
        otherChannel.setId(OTHER_CHANNEL_ID);
    }

    private static Map<String, String> payload(String content) {
        Map<String, String> payload = new HashMap<>();
        payload.put("content", content);
        return payload;
    }

    private static Map<String, String> forgedPayload(String username, String content) {
        Map<String, String> payload = payload(content);
        payload.put("username", username);
        return payload;
    }

    // --- /chat.send ---

    @Test
    void testSendMessage_SavesTrimmedMessageToDefaultChannel() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        ChatMessage saved = new ChatMessage("alice", "hello", DEFAULT_CHANNEL_ID);
        when(chatService.saveMessage("alice", "hello", DEFAULT_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.sendMessage(payload("  hello  "), ALICE);

        assertSame(saved, result);
        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
    }

    @Test
    void testSendMessage_IgnoresChannelIdInPayload() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        Map<String, String> payload = payload("hello");
        payload.put("channelId", String.valueOf(OTHER_CHANNEL_ID));

        chatController.sendMessage(payload, ALICE);

        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
        verify(channelService, never()).getChannelById(anyLong());
    }

    @Test
    void testSendMessage_NullPayload() {
        assertThrows(IllegalArgumentException.class, () -> chatController.sendMessage(null, ALICE));
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_WhitespaceOnlyContent() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("   "), ALICE));

        assertEquals("Invalid message content", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_ContentAtMaxLengthIsAccepted() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        String content = "a".repeat(1000);

        chatController.sendMessage(payload(content), ALICE);

        verify(chatService).saveMessage("alice", content, DEFAULT_CHANNEL_ID);
    }

    @Test
    void testSendMessage_ContentOverMaxLengthIsRejected() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessage(payload("a".repeat(1001)), ALICE));

        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessage_AuthorIsThePrincipalNotThePayload() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        chatController.sendMessage(forgedPayload("mallory", "hello"), ALICE);

        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
    }

    @Test
    void testSendMessage_ReservedNameInPayloadIsIgnored() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);

        chatController.sendMessage(forgedPayload("System", "hello"), ALICE);

        verify(chatService).saveMessage("alice", "hello", DEFAULT_CHANNEL_ID);
    }

    // --- /chat.send/{channelId} ---

    @Test
    void testSendMessageToChannel_AuthorIsThePrincipalNotThePayload() {
        when(channelService.getChannelById(OTHER_CHANNEL_ID)).thenReturn(Optional.of(otherChannel));

        chatController.sendMessageToChannel(OTHER_CHANNEL_ID, forgedPayload("mallory", "hello"), ALICE);

        verify(chatService).saveMessage("alice", "hello", OTHER_CHANNEL_ID);
    }

    @Test
    void testSendMessageToChannel_SavesTrimmedMessageToThatChannel() {
        when(channelService.getChannelById(OTHER_CHANNEL_ID)).thenReturn(Optional.of(otherChannel));
        ChatMessage saved = new ChatMessage("alice", "hello", OTHER_CHANNEL_ID);
        when(chatService.saveMessage("alice", "hello", OTHER_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.sendMessageToChannel(OTHER_CHANNEL_ID, payload(" hello "), ALICE);

        assertSame(saved, result);
        verify(channelService, never()).getOrCreateDefaultChannel();
    }

    @Test
    void testSendMessageToChannel_ChannelNotFound() {
        when(channelService.getChannelById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(999L, payload("hello"), ALICE));

        assertEquals("Channel does not exist", ex.getMessage());
        verifyNoInteractions(chatService);
    }

    @Test
    void testSendMessageToChannel_NullPayload() {
        assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(OTHER_CHANNEL_ID, null, ALICE));
        verifyNoInteractions(chatService, channelService);
    }

    @Test
    void testSendMessageToChannel_MissingContentRejectedBeforeChannelLookup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.sendMessageToChannel(OTHER_CHANNEL_ID, payload(null), ALICE));

        assertEquals("Invalid message content", ex.getMessage());
        verifyNoInteractions(chatService, channelService);
    }

    // --- /chat.join ---

    @Test
    void testUserJoin_SavesSystemMessageToDefaultChannel() {
        when(channelService.getOrCreateDefaultChannel()).thenReturn(defaultChannel);
        ChatMessage saved = new ChatMessage("System", "alice has joined the chat", DEFAULT_CHANNEL_ID);
        when(chatService.saveMessage("System", "alice has joined the chat", DEFAULT_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.userJoin(ALICE);

        assertSame(saved, result);
    }

    // --- /chat.join/{channelId} ---

    @Test
    void testUserJoinChannel_SavesSystemMessageToThatChannel() {
        when(channelService.getChannelById(OTHER_CHANNEL_ID)).thenReturn(Optional.of(otherChannel));
        ChatMessage saved = new ChatMessage("System", "alice has joined the chat", OTHER_CHANNEL_ID);
        when(chatService.saveMessage("System", "alice has joined the chat", OTHER_CHANNEL_ID)).thenReturn(saved);

        ChatMessage result = chatController.userJoinChannel(OTHER_CHANNEL_ID, ALICE);

        assertSame(saved, result);
        verify(channelService, never()).getOrCreateDefaultChannel();
    }

    @Test
    void testUserJoinChannel_ChannelNotFound() {
        when(channelService.getChannelById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> chatController.userJoinChannel(999L, ALICE));

        assertEquals("Channel does not exist", ex.getMessage());
        verifyNoInteractions(chatService);
    }

}
