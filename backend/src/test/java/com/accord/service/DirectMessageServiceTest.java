package com.accord.service;

import com.accord.model.DirectMessage;
import com.accord.repository.DirectMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @InjectMocks
    private DirectMessageService directMessageService;

    private DirectMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new DirectMessage(1L, 2L, "Hello");
        testMessage.setId(1L);
    }

    @Test
    void testSendMessage() {
        when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(testMessage);

        DirectMessage result = directMessageService.sendMessage(1L, 2L, "Hello");

        assertNotNull(result);
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getRecipientId());
        assertEquals("Hello", result.getContent());
        verify(directMessageRepository, times(1)).save(any(DirectMessage.class));
    }

    @Test
    void testGetConversation() {
        DirectMessage msg1 = new DirectMessage(1L, 2L, "Hi");
        DirectMessage msg2 = new DirectMessage(2L, 1L, "Hello");
        List<DirectMessage> messages = Arrays.asList(msg2, msg1);

        when(directMessageRepository.findConversation(eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(messages);

        List<DirectMessage> result = directMessageService.getConversation(1L, 2L, 50);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify reversed (oldest first)
        assertEquals("Hi", result.get(0).getContent());
        assertEquals("Hello", result.get(1).getContent());
    }

    @Test
    void testGetConversation_Empty() {
        when(directMessageRepository.findConversation(eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<DirectMessage> result = directMessageService.getConversation(1L, 2L, 50);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testMarkAsRead() {
        DirectMessage msg = new DirectMessage(1L, 2L, "Read me");
        msg.setId(10L);
        when(directMessageRepository.findById(10L)).thenReturn(Optional.of(msg));
        when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(msg);

        directMessageService.markAsRead(10L, 2L);

        assertTrue(msg.isRead());
        verify(directMessageRepository).save(msg);
    }

    @Test
    void testMarkAsRead_WrongRecipient() {
        DirectMessage msg = new DirectMessage(1L, 2L, "Not yours");
        msg.setId(10L);
        when(directMessageRepository.findById(10L)).thenReturn(Optional.of(msg));

        directMessageService.markAsRead(10L, 3L); // user 3 is not the recipient

        assertFalse(msg.isRead());
        verify(directMessageRepository, never()).save(any());
    }

    @Test
    void testMarkConversationAsRead() {
        when(directMessageRepository.markConversationAsRead(2L, 1L)).thenReturn(2);

        directMessageService.markConversationAsRead(2L, 1L);

        verify(directMessageRepository).markConversationAsRead(2L, 1L);
    }

    @Test
    void testGetUnreadCount() {
        when(directMessageRepository.countUnreadForUser(2L)).thenReturn(5L);

        long count = directMessageService.getUnreadCount(2L);

        assertEquals(5L, count);
    }

    @Test
    void testGetUnreadCountFromSender() {
        when(directMessageRepository.countUnreadFromSender(2L, 1L)).thenReturn(3L);

        long count = directMessageService.getUnreadCountFromSender(2L, 1L);

        assertEquals(3L, count);
    }

    @Test
    void testGetConversationPartnerIds() {
        when(directMessageRepository.findConversationPartnerIds(1L))
                .thenReturn(Arrays.asList(2L, 3L, 4L));

        List<Long> partnerIds = directMessageService.getConversationPartnerIds(1L);

        assertEquals(3, partnerIds.size());
        assertTrue(partnerIds.contains(2L));
        assertTrue(partnerIds.contains(3L));
        assertTrue(partnerIds.contains(4L));
    }
}
