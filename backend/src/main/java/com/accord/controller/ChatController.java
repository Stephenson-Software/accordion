package com.accord.controller;

import com.accord.model.ChatMessage;
import com.accord.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(Map<String, String> payload) {
        String username = payload.get("username");
        String content = payload.get("content");
        
        return chatService.saveMessage(username, content);
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/messages")
    public ChatMessage userJoin(Map<String, String> payload) {
        String username = payload.get("username");
        ChatMessage joinMessage = new ChatMessage("System", username + " has joined the chat");
        return chatService.saveMessage("System", username + " has joined the chat");
    }
}

@RestController
@CrossOrigin(origins = "*")
class MessageRestController {
    
    @Autowired
    private ChatService chatService;

    @GetMapping("/api/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = chatService.getRecentMessages(limit);
        return ResponseEntity.ok(messages);
    }
}
