package com.accord.controller;

import com.accord.model.Channel;
import com.accord.service.ChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChannelController.class)
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChannelService channelService;

    @Test
    void testGetAllChannels() throws Exception {
        Channel channel1 = new Channel("general", "General discussion", "System");
        Channel channel2 = new Channel("random", "Random stuff", "Admin");

        when(channelService.getAllChannels()).thenReturn(Arrays.asList(channel1, channel2));

        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("general"))
                .andExpect(jsonPath("$[1].name").value("random"));

        verify(channelService).getAllChannels();
    }

    @Test
    void testGetChannelById() throws Exception {
        Channel channel = new Channel("general", "General discussion", "System");

        when(channelService.getChannelById(1L)).thenReturn(Optional.of(channel));

        mockMvc.perform(get("/api/channels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("general"))
                .andExpect(jsonPath("$.description").value("General discussion"));

        verify(channelService).getChannelById(1L);
    }

    @Test
    void testGetChannelByIdNotFound() throws Exception {
        when(channelService.getChannelById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/channels/999"))
                .andExpect(status().isNotFound());

        verify(channelService).getChannelById(999L);
    }

    @Test
    void testCreateChannel() throws Exception {
        Channel channel = new Channel("tech-talk", "Tech discussions", "TestUser");
        when(channelService.createChannel(eq("tech-talk"), eq("Tech discussions"), eq("TestUser")))
                .thenReturn(channel);

        Map<String, String> payload = new HashMap<>();
        payload.put("name", "tech-talk");
        payload.put("description", "Tech discussions");
        payload.put("createdBy", "TestUser");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("tech-talk"))
                .andExpect(jsonPath("$.description").value("Tech discussions"));

        verify(channelService).createChannel("tech-talk", "Tech discussions", "TestUser");
    }

    @Test
    void testCreateChannelWithInvalidName() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "ab"); // Too short
        payload.put("description", "Test");
        payload.put("createdBy", "TestUser");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(channelService, never()).createChannel(any(), any(), any());
    }

    @Test
    void testCreateChannelWithInvalidCharacters() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "test channel!"); // Contains space and special char
        payload.put("description", "Test");
        payload.put("createdBy", "TestUser");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(channelService, never()).createChannel(any(), any(), any());
    }

    @Test
    void testCreateChannelWithMissingName() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("description", "Test");
        payload.put("createdBy", "TestUser");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(channelService, never()).createChannel(any(), any(), any());
    }

    @Test
    void testCreateChannelWithDuplicateName() throws Exception {
        when(channelService.createChannel(eq("general"), any(), any()))
                .thenThrow(new IllegalArgumentException("Channel with name 'general' already exists"));

        Map<String, String> payload = new HashMap<>();
        payload.put("name", "general");
        payload.put("description", "Test");
        payload.put("createdBy", "TestUser");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Channel with name 'general' already exists"));

        verify(channelService).createChannel("general", "Test", "TestUser");
    }
}
