package com.accord.config;

import com.accord.service.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ChannelService channelService;

    @Override
    public void run(String... args) {
        // Ensure default "general" channel exists
        channelService.getOrCreateDefaultChannel();
    }
}
