package com.accordion.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTest {

    @Test
    void testChannelCreation_NoArgs() {
        LocalDateTime before = LocalDateTime.now();
        Channel channel = new Channel();

        assertNull(channel.getId());
        assertNull(channel.getName());
        assertNull(channel.getDescription());
        assertNull(channel.getCreatedBy());
        assertNotNull(channel.getCreatedAt());
        assertFalse(channel.getCreatedAt().isBefore(before));
        assertTrue(channel.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testChannelCreation_WithArgs() {
        LocalDateTime before = LocalDateTime.now();
        Channel channel = new Channel("random", "Off-topic chat", "alice");

        assertNull(channel.getId());
        assertEquals("random", channel.getName());
        assertEquals("Off-topic chat", channel.getDescription());
        assertEquals("alice", channel.getCreatedBy());
        assertNotNull(channel.getCreatedAt());
        assertFalse(channel.getCreatedAt().isBefore(before));
        assertTrue(channel.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testChannelCreation_WithNullDescription() {
        Channel channel = new Channel("random", null, "alice");

        assertEquals("random", channel.getName());
        assertNull(channel.getDescription());
        assertEquals("alice", channel.getCreatedBy());
    }

    @Test
    void testChannelSetters() {
        Channel channel = new Channel();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);

        channel.setId(7L);
        channel.setName("announcements");
        channel.setDescription("Server news");
        channel.setCreatedBy("bob");
        channel.setCreatedAt(createdAt);

        assertEquals(7L, channel.getId());
        assertEquals("announcements", channel.getName());
        assertEquals("Server news", channel.getDescription());
        assertEquals("bob", channel.getCreatedBy());
        assertEquals(createdAt, channel.getCreatedAt());
    }

    @Test
    void testChannelSetters_OverrideConstructorValues() {
        Channel channel = new Channel("random", "Off-topic chat", "alice");
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);

        channel.setName("general");
        channel.setDescription(null);
        channel.setCreatedBy("system");
        channel.setCreatedAt(createdAt);

        assertEquals("general", channel.getName());
        assertNull(channel.getDescription());
        assertEquals("system", channel.getCreatedBy());
        assertEquals(createdAt, channel.getCreatedAt());
    }
}
