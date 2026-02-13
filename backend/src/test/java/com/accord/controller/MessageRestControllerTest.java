package com.accord.controller;

import com.accord.model.ChatMessage;
import com.accord.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageRestController.class)
@TestPropertySource(properties = {
    "app.cors.allowed-origins=*"
})
class MessageRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    private List<ChatMessage> testMessages;

    @BeforeEach
    void setUp() {
        ChatMessage msg1 = new ChatMessage("user1", "Hello");
        ChatMessage msg2 = new ChatMessage("user2", "World");
        testMessages = Arrays.asList(msg1, msg2);
    }

    @Test
    void testGetMessages_DefaultLimit() throws Exception {
        when(chatService.getRecentMessages(50)).thenReturn(testMessages);

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[0].content").value("Hello"));

        verify(chatService, times(1)).getRecentMessages(50);
    }

    @Test
    void testGetMessages_CustomLimit() throws Exception {
        when(chatService.getRecentMessages(10)).thenReturn(testMessages);

        mockMvc.perform(get("/api/messages")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(chatService, times(1)).getRecentMessages(10);
    }

    @Test
    void testGetMessages_LimitTooHigh() throws Exception {
        when(chatService.getRecentMessages(500)).thenReturn(testMessages);

        mockMvc.perform(get("/api/messages")
                .param("limit", "1000"))
                .andExpect(status().isOk());

        // Should cap at 500
        verify(chatService, times(1)).getRecentMessages(500);
    }

    @Test
    void testGetMessages_LimitTooLow() throws Exception {
        when(chatService.getRecentMessages(1)).thenReturn(testMessages);

        mockMvc.perform(get("/api/messages")
                .param("limit", "0"))
                .andExpect(status().isOk());

        // Should set to minimum 1
        verify(chatService, times(1)).getRecentMessages(1);
    }

    @Test
    void testGetMessages_NegativeLimit() throws Exception {
        when(chatService.getRecentMessages(1)).thenReturn(testMessages);

        mockMvc.perform(get("/api/messages")
                .param("limit", "-5"))
                .andExpect(status().isOk());

        // Should set to minimum 1
        verify(chatService, times(1)).getRecentMessages(1);
    }

    @Test
    void testGetMessages_EmptyResult() throws Exception {
        when(chatService.getRecentMessages(anyInt())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(chatService, times(1)).getRecentMessages(50);
    }
}
