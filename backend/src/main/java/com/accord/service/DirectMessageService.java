package com.accord.service;

import com.accord.model.DirectMessage;
import com.accord.repository.DirectMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DirectMessageService {

    @Autowired
    private DirectMessageRepository directMessageRepository;

    public DirectMessage sendMessage(Long senderId, Long recipientId, String content) {
        DirectMessage message = new DirectMessage(senderId, recipientId, content);
        return directMessageRepository.save(message);
    }

    public List<DirectMessage> getConversation(Long userId1, Long userId2, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<DirectMessage> messages = directMessageRepository.findConversation(userId1, userId2, pageable);
        Collections.reverse(messages); // Show oldest first
        return messages;
    }

    public void markAsRead(Long messageId, Long recipientId) {
        directMessageRepository.findById(messageId).ifPresent(message -> {
            if (message.getRecipientId().equals(recipientId)) {
                message.setRead(true);
                directMessageRepository.save(message);
            }
        });
    }

    public void markConversationAsRead(Long recipientId, Long senderId) {
        List<DirectMessage> unreadMessages = directMessageRepository.findByRecipientIdAndReadFalse(recipientId);
        for (DirectMessage message : unreadMessages) {
            if (message.getSenderId().equals(senderId)) {
                message.setRead(true);
                directMessageRepository.save(message);
            }
        }
    }

    public long getUnreadCount(Long recipientId) {
        return directMessageRepository.countUnreadForUser(recipientId);
    }

    public long getUnreadCountFromSender(Long recipientId, Long senderId) {
        return directMessageRepository.countUnreadFromSender(recipientId, senderId);
    }

    public List<Long> getConversationPartnerIds(Long userId) {
        return directMessageRepository.findConversationPartnerIds(userId);
    }
}
