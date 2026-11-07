package com.accordion.controller;

import com.accordion.model.ChatMessage;
import com.accordion.model.TypingIndicator;
import com.accordion.service.ChatService;
import com.accordion.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    @Value("${app.message.max-length}")
    private int maxMessageLength;

    @Autowired
    private ChatService chatService;

    @Autowired
    private com.accordion.service.ChannelService channelService;

    // Every handler takes the author from the session's Principal. A "username" in the payload
    // is client-supplied and ignored. WebSocketAuthInterceptor drops any SEND without a session user.

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(Map<String, String> payload, Principal principal) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }

        String content = payload.get("content");

        // Legacy /chat.send always uses the default channel (ignore any channelId in payload)
        Long channelId = channelService.getOrCreateDefaultChannel().getId();

        if (!ValidationUtils.isValidContent(content, maxMessageLength)) {
            throw new IllegalArgumentException("Invalid message content");
        }

        // Trim content before saving to ensure consistency
        return chatService.saveMessage(principal.getName(), content.trim(), channelId);
    }

    @MessageMapping("/chat.send/{channelId}")
    @SendTo("/topic/messages/{channelId}")
    public ChatMessage sendMessageToChannel(@DestinationVariable Long channelId,
                                           Map<String, String> payload,
                                           Principal principal) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }

        String content = payload.get("content");

        if (!ValidationUtils.isValidContent(content, maxMessageLength)) {
            throw new IllegalArgumentException("Invalid message content");
        }

        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }

        // Trim content before saving to ensure consistency
        return chatService.saveMessage(principal.getName(), content.trim(), channelId);
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/messages")
    public ChatMessage userJoin(Principal principal) {
        // Legacy /chat.join sends to the global topic; always associate with the default channel
        Long channelId = channelService.getOrCreateDefaultChannel().getId();

        return chatService.saveMessage("System", principal.getName() + " has joined the chat", channelId);
    }

    @MessageMapping("/chat.join/{channelId}")
    @SendTo("/topic/messages/{channelId}")
    public ChatMessage userJoinChannel(@DestinationVariable Long channelId, Principal principal) {
        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }

        return chatService.saveMessage("System", principal.getName() + " has joined the chat", channelId);
    }

    @MessageMapping("/chat.typing/{channelId}")
    @SendTo("/topic/typing/{channelId}")
    public TypingIndicator userTyping(@DestinationVariable Long channelId,
                                     Map<String, Object> payload,
                                     Principal principal) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null");
        }

        Boolean typing = (Boolean) payload.get("typing");

        if (typing == null) {
            typing = true; // Default to typing=true if not specified
        }

        // Verify channel exists
        if (!channelService.getChannelById(channelId).isPresent()) {
            throw new IllegalArgumentException("Channel does not exist");
        }

        return new TypingIndicator(principal.getName(), channelId, typing);
    }
}

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
class MessageRestController {
    
    private static final int MAX_LIMIT = 500;

    @Autowired
    private ChatService chatService;

    @GetMapping("/api/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long channelId) {
        
        if (limit < 1) {
            limit = 1;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        
        List<ChatMessage> messages;
        if (channelId != null) {
            messages = chatService.getRecentMessagesByChannel(channelId, limit);
        } else {
            messages = chatService.getRecentMessages(limit);
        }
        return ResponseEntity.ok(messages);
    }
}
