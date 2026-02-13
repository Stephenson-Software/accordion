package com.accord.webapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @Value("${accord.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${accord.backend.ws.url:ws://localhost:8080/ws}")
    private String backendWsUrl;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("backendUrl", backendUrl);
        model.addAttribute("backendWsUrl", backendWsUrl);
        return "index";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("backendUrl", backendUrl);
        model.addAttribute("backendWsUrl", backendWsUrl);
        return "chat";
    }
}
