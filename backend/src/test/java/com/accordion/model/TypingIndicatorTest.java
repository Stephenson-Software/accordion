package com.accordion.model;

import com.accordion.config.JacksonConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypingIndicatorTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void testTypingIndicatorCreation_NoArgs() {
        TypingIndicator indicator = new TypingIndicator();

        assertNull(indicator.getUsername());
        assertNull(indicator.getChannelId());
        assertFalse(indicator.isTyping());
    }

    @Test
    void testTypingIndicatorCreation_WithArgs() {
        TypingIndicator indicator = new TypingIndicator("alice", 3L, true);

        assertEquals("alice", indicator.getUsername());
        assertEquals(3L, indicator.getChannelId());
        assertTrue(indicator.isTyping());
    }

    @Test
    void testTypingIndicatorSetters_OverrideConstructorValues() {
        TypingIndicator indicator = new TypingIndicator("alice", 3L, true);

        indicator.setUsername("bob");
        indicator.setChannelId(9L);
        indicator.setTyping(false);

        assertEquals("bob", indicator.getUsername());
        assertEquals(9L, indicator.getChannelId());
        assertFalse(indicator.isTyping());
    }

    @Test
    void testSerializesWithPropertyNamesTheWebClientReads() throws Exception {
        // chat.html's onTypingReceived reads `username` and `typing` from the
        // /topic/typing/{channelId} payload, so the boolean must not be
        // serialized as `isTyping`.
        TypingIndicator indicator = new TypingIndicator("alice", 3L, true);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(indicator));

        assertEquals(3, json.size(), "Unexpected properties in: " + json);
        assertEquals("alice", json.get("username").asText());
        assertEquals(3L, json.get("channelId").asLong());
        assertTrue(json.get("typing").isBoolean());
        assertTrue(json.get("typing").asBoolean());
        assertFalse(json.has("isTyping"));
    }

    @Test
    void testSerializesNullFieldsAsJsonNull() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new TypingIndicator()));

        assertTrue(json.get("username").isNull());
        assertTrue(json.get("channelId").isNull());
        assertFalse(json.get("typing").asBoolean());
    }

    @Test
    void testCanBeRoundTripped() throws Exception {
        TypingIndicator original = new TypingIndicator("alice", 3L, true);

        TypingIndicator deserialized = objectMapper.readValue(
            objectMapper.writeValueAsString(original), TypingIndicator.class);

        assertEquals("alice", deserialized.getUsername());
        assertEquals(3L, deserialized.getChannelId());
        assertTrue(deserialized.isTyping());
    }
}
