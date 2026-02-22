package com.accordion.webapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    // Client-side backend URL (used by browser JavaScript)
    // This must be accessible from the user's browser, not just from Docker network
    @Value("${accordion.backend.client.url:http://localhost:8080}")
    private String backendClientUrl;

    @Value("${accordion.backend.client.ws.url:http://localhost:8080/ws}")
    private String backendClientWsUrl;

    @GetMapping("/")
    public String index(Model model) {
        // Use client URLs for browser-side JavaScript
        model.addAttribute("backendUrl", backendClientUrl);
        model.addAttribute("backendWsUrl", backendClientWsUrl);
        return "index";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        // Use client URLs for browser-side JavaScript
        model.addAttribute("backendUrl", backendClientUrl);
        model.addAttribute("backendWsUrl", backendClientWsUrl);
        return "chat";
    }
}
