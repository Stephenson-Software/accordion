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

    public ChatMessage saveMessage(String username, String content) {
        ChatMessage message = new ChatMessage(username, content);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findAllByOrderByTimestampDesc(pageable);
        Collections.reverse(messages); // Show oldest first
        return messages;
    }
}
