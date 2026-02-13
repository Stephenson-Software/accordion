package com.accord.service;

import com.accord.model.ChatMessage;
import com.accord.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatService chatService;

    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new ChatMessage("testuser", "Hello world");
        testMessage.setId(1L);
    }

    @Test
    void testSaveMessage() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

        ChatMessage result = chatService.saveMessage("testuser", "Hello world");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("Hello world", result.getContent());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testGetRecentMessages() {
        ChatMessage msg1 = new ChatMessage("user1", "Message 1");
        ChatMessage msg2 = new ChatMessage("user2", "Message 2");
        ChatMessage msg3 = new ChatMessage("user3", "Message 3");
        
        List<ChatMessage> messages = Arrays.asList(msg3, msg2, msg1);
        
        when(chatMessageRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(messages);

        List<ChatMessage> result = chatService.getRecentMessages(3);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Verify the list is reversed (oldest first)
        assertEquals("Message 1", result.get(0).getContent());
        assertEquals("Message 3", result.get(2).getContent());
        
        verify(chatMessageRepository, times(1)).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    void testGetRecentMessages_EmptyList() {
        when(chatMessageRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(Arrays.asList());

        List<ChatMessage> result = chatService.getRecentMessages(50);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(chatMessageRepository, times(1)).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    void testGetRecentMessages_WithLimit() {
        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage("user1", "msg1"),
                new ChatMessage("user2", "msg2")
        );
        
        when(chatMessageRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(messages);

        chatService.getRecentMessages(10);

        verify(chatMessageRepository).findAllByOrderByTimestampDesc(
                argThat(pageable -> pageable.getPageSize() == 10)
        );
    }
}
