package com.accordion.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@TestPropertySource(properties = {
    "accordion.backend.client.url=http://backend.example:9090",
    "accordion.backend.client.ws.url=http://backend.example:9090/ws"
})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndexReturnsIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testIndexExposesConfiguredClientUrls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(model().attribute("backendUrl", "http://backend.example:9090"))
                .andExpect(model().attribute("backendWsUrl", "http://backend.example:9090/ws"));
    }

    @Test
    void testChatReturnsChatView() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"));
    }

    @Test
    void testChatExposesConfiguredClientUrls() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(model().attribute("backendUrl", "http://backend.example:9090"))
                .andExpect(model().attribute("backendWsUrl", "http://backend.example:9090/ws"));
    }

    @Test
    void testUnmappedPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
