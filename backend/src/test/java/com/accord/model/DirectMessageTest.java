package com.accord.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DirectMessageTest {

    @Test
    void testDirectMessageCreation_NoArgs() {
        DirectMessage message = new DirectMessage();

        assertNull(message.getId());
        assertNull(message.getSenderId());
        assertNull(message.getRecipientId());
        assertNull(message.getContent());
        assertNotNull(message.getTimestamp());
        assertFalse(message.isRead());
        assertTrue(message.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testDirectMessageCreation_WithArgs() {
        DirectMessage message = new DirectMessage(1L, 2L, "Hello there");

        assertNull(message.getId());
        assertEquals(1L, message.getSenderId());
        assertEquals(2L, message.getRecipientId());
        assertEquals("Hello there", message.getContent());
        assertNotNull(message.getTimestamp());
        assertFalse(message.isRead());
    }

    @Test
    void testDirectMessageSetters() {
        DirectMessage message = new DirectMessage();
        LocalDateTime now = LocalDateTime.now();

        message.setId(1L);
        message.setSenderId(10L);
        message.setRecipientId(20L);
        message.setContent("Test DM");
        message.setTimestamp(now);
        message.setRead(true);

        assertEquals(1L, message.getId());
        assertEquals(10L, message.getSenderId());
        assertEquals(20L, message.getRecipientId());
        assertEquals("Test DM", message.getContent());
        assertEquals(now, message.getTimestamp());
        assertTrue(message.isRead());
    }

    @Test
    void testDirectMessage_LongContent() {
        String longContent = "a".repeat(1000);
        DirectMessage message = new DirectMessage(1L, 2L, longContent);

        assertEquals(1000, message.getContent().length());
        assertEquals(longContent, message.getContent());
    }

    @Test
    void testDirectMessage_ReadDefaultsFalse() {
        DirectMessage message = new DirectMessage(1L, 2L, "Unread message");
        assertFalse(message.isRead());
    }

    @Test
    void testDirectMessage_MarkAsRead() {
        DirectMessage message = new DirectMessage(1L, 2L, "Read me");
        assertFalse(message.isRead());

        message.setRead(true);
        assertTrue(message.isRead());
    }
}
