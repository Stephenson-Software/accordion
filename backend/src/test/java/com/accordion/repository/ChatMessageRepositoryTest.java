package com.accordion.repository;

import com.accordion.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterizes the derived queries ChatService relies on for message history:
 * newest-first ordering, the page-size limit, and scoping to a single channel.
 * Runs against the embedded H2 database that @DataJpaTest provisions per test.
 */
@DataJpaTest
class ChatMessageRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private ChatMessage saveMessage(String content, Long channelId, int minutesAfterBase) {
        ChatMessage message = new ChatMessage("alice", content, channelId);
        message.setTimestamp(BASE.plusMinutes(minutesAfterBase));
        return chatMessageRepository.save(message);
    }

    @Test
    void findAllByOrderByTimestampDescReturnsNewestFirstRegardlessOfInsertionOrder() {
        saveMessage("middle", 1L, 5);
        saveMessage("oldest", 1L, 0);
        saveMessage("newest", 2L, 10);

        List<ChatMessage> result = chatMessageRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 10));

        assertEquals(List.of("newest", "middle", "oldest"),
                result.stream().map(ChatMessage::getContent).toList());
    }

    @Test
    void findAllByOrderByTimestampDescHonoursPageSize() {
        saveMessage("first", 1L, 0);
        saveMessage("second", 1L, 1);
        saveMessage("third", 1L, 2);

        List<ChatMessage> result = chatMessageRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 2));

        assertEquals(List.of("third", "second"),
                result.stream().map(ChatMessage::getContent).toList());
    }

    @Test
    void findAllByOrderByTimestampDescReturnsEmptyListWhenNoMessages() {
        List<ChatMessage> result = chatMessageRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByChannelIdOrderByTimestampDescReturnsOnlyThatChannelNewestFirst() {
        saveMessage("general-old", 1L, 0);
        saveMessage("random-old", 2L, 1);
        saveMessage("general-new", 1L, 2);
        saveMessage("random-new", 2L, 3);

        List<ChatMessage> result = chatMessageRepository.findByChannelIdOrderByTimestampDesc(2L, PageRequest.of(0, 10));

        assertEquals(List.of("random-new", "random-old"),
                result.stream().map(ChatMessage::getContent).toList());
    }

    @Test
    void findByChannelIdOrderByTimestampDescHonoursPageSize() {
        saveMessage("one", 3L, 0);
        saveMessage("two", 3L, 1);
        saveMessage("three", 3L, 2);
        saveMessage("other-channel", 4L, 3);

        List<ChatMessage> result = chatMessageRepository.findByChannelIdOrderByTimestampDesc(3L, PageRequest.of(0, 1));

        assertEquals(List.of("three"),
                result.stream().map(ChatMessage::getContent).toList());
    }

    @Test
    void findByChannelIdOrderByTimestampDescReturnsEmptyListForUnknownChannel() {
        saveMessage("general", 1L, 0);

        List<ChatMessage> result = chatMessageRepository.findByChannelIdOrderByTimestampDesc(99L, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByChannelIdOrderByTimestampDescExcludesMessagesWithNullChannel() {
        saveMessage("unscoped", null, 0);
        saveMessage("general", 1L, 1);

        List<ChatMessage> result = chatMessageRepository.findByChannelIdOrderByTimestampDesc(1L, PageRequest.of(0, 10));

        assertEquals(List.of("general"),
                result.stream().map(ChatMessage::getContent).toList());
    }
}
