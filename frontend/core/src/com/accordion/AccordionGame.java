package com.accordion;

import com.accordion.screen.LoginScreen;
import com.accordion.service.ApiClient;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class AccordionGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    
    // Shared API client for REST calls (holds JWT token)
    public final ApiClient apiClient = new ApiClient();
    
    // Authenticated user info
    public String username;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont(); // Use LibGDX default font
        setScreen(new LoginScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
