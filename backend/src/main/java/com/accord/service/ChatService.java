package com.accord.service;

import com.accord.model.ChatMessage;
import com.accord.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(String username, String content, Long channelId) {
        ChatMessage message = new ChatMessage(username, content, channelId);
        return chatMessageRepository.save(message);
    }

    public ChatMessage saveMessage(String username, String content) {
        // For backwards compatibility, default to channel 1 (general)
        return saveMessage(username, content, 1L);
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findAllByOrderByTimestampDesc(pageable);
        Collections.reverse(messages); // Show oldest first
        return messages;
    }

    public List<ChatMessage> getRecentMessagesByChannel(Long channelId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findByChannelIdOrderByTimestampDesc(channelId, pageable);
        Collections.reverse(messages); // Show oldest first
        return messages;
    }
}
