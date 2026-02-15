package com.accord.controller;

import com.accord.model.DirectMessage;
import com.accord.model.User;
import com.accord.service.DirectMessageService;
import com.accord.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DirectMessageController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.accord.config.SecurityConfig.class,
                   com.accord.security.JwtAuthenticationFilter.class,
                   com.accord.security.WebSocketAuthInterceptor.class}))
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "app.cors.allowed-origins=*",
    "app.message.max-length=1000"
})
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DirectMessageService directMessageService;

    @MockBean
    private UserService userService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        sender = new User("alice");
        sender.setId(1L);
        recipient = new User("bob");
        recipient.setId(2L);
    }

    @Test
    void testSendDirectMessage_Success() throws Exception {
        DirectMessage dm = new DirectMessage(1L, 2L, "Hello Bob");
        dm.setId(1L);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(directMessageService.sendMessage(1L, 2L, "Hello Bob")).thenReturn(dm);

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderUsername", "alice");
        payload.put("recipientUsername", "bob");
        payload.put("content", "Hello Bob");

        mockMvc.perform(post("/api/dm/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value(1))
                .andExpect(jsonPath("$.recipientId").value(2))
                .andExpect(jsonPath("$.content").value("Hello Bob"));

        verify(messagingTemplate).convertAndSend(eq("/user/2/queue/messages"), any(DirectMessage.class));
    }

    @Test
    void testSendDirectMessage_MissingSender() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientUsername", "bob");
        payload.put("content", "Hello");

        mockMvc.perform(post("/api/dm/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendDirectMessage_SelfMessage() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderUsername", "alice");
        payload.put("recipientUsername", "alice");
        payload.put("content", "Hello me");

        mockMvc.perform(post("/api/dm/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot send a message to yourself"));
    }

    @Test
    void testSendDirectMessage_RecipientNotFound() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderUsername", "alice");
        payload.put("recipientUsername", "unknown");
        payload.put("content", "Hello");

        mockMvc.perform(post("/api/dm/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Recipient not found"));
    }

    @Test
    void testGetConversation_Success() throws Exception {
        DirectMessage dm1 = new DirectMessage(1L, 2L, "Hi");
        DirectMessage dm2 = new DirectMessage(2L, 1L, "Hey");
        List<DirectMessage> messages = Arrays.asList(dm1, dm2);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(directMessageService.getConversation(1L, 2L, 50)).thenReturn(messages);

        mockMvc.perform(get("/api/dm/conversation")
                .param("user1", "alice")
                .param("user2", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetConversation_UserNotFound() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dm/conversation")
                .param("user1", "alice")
                .param("user2", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarkAsRead() throws Exception {
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));

        mockMvc.perform(post("/api/dm/read/1")
                .param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("marked as read"));

        verify(directMessageService).markAsRead(1L, 2L);
    }

    @Test
    void testGetUnreadCount() throws Exception {
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(directMessageService.getUnreadCount(2L)).thenReturn(5L);

        mockMvc.perform(get("/api/dm/unread")
                .param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void testGetUnreadCountFromSender() throws Exception {
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(directMessageService.getUnreadCountFromSender(2L, 1L)).thenReturn(3L);

        mockMvc.perform(get("/api/dm/unread/from")
                .param("username", "bob")
                .param("senderUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void testMarkConversationAsRead() throws Exception {
        when(userService.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(userService.findByUsername("alice")).thenReturn(Optional.of(sender));

        Map<String, String> payload = new HashMap<>();
        payload.put("recipientUsername", "bob");
        payload.put("senderUsername", "alice");

        mockMvc.perform(post("/api/dm/read/conversation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("conversation marked as read"));

        verify(directMessageService).markConversationAsRead(2L, 1L);
    }

    @Test
    void testSendDirectMessage_InvalidContent() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderUsername", "alice");
        payload.put("recipientUsername", "bob");
        payload.put("content", "");

        mockMvc.perform(post("/api/dm/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid message content"));
    }
}
