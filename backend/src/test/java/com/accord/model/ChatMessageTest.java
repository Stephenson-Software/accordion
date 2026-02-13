package com.accord.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void testChatMessageCreation_NoArgs() {
        ChatMessage message = new ChatMessage();
        
        assertNull(message.getId());
        assertNull(message.getUsername());
        assertNull(message.getContent());
        assertNotNull(message.getTimestamp());
        assertTrue(message.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testChatMessageCreation_WithArgs() {
        ChatMessage message = new ChatMessage("testuser", "Hello world");
        
        assertNull(message.getId());
        assertEquals("testuser", message.getUsername());
        assertEquals("Hello world", message.getContent());
        assertNotNull(message.getTimestamp());
        assertTrue(message.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testChatMessageSetters() {
        ChatMessage message = new ChatMessage();
        LocalDateTime now = LocalDateTime.now();
        
        message.setId(1L);
        message.setUsername("user1");
        message.setContent("Test message");
        message.setTimestamp(now);
        
        assertEquals(1L, message.getId());
        assertEquals("user1", message.getUsername());
        assertEquals("Test message", message.getContent());
        assertEquals(now, message.getTimestamp());
    }

    @Test
    void testChatMessageGetters() {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage("user2", "Another message");
        message.setId(10L);
        message.setTimestamp(now);
        
        assertEquals(10L, message.getId());
        assertEquals("user2", message.getUsername());
        assertEquals("Another message", message.getContent());
        assertEquals(now, message.getTimestamp());
    }

    @Test
    void testChatMessage_LongContent() {
        String longContent = "a".repeat(1000);
        ChatMessage message = new ChatMessage("user", longContent);
        
        assertEquals(1000, message.getContent().length());
        assertEquals(longContent, message.getContent());
    }
}
