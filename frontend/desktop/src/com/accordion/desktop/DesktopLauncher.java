package com.accordion.desktop;

import com.accordion.AccordionGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Accordion Chat");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        
        new Lwjgl3Application(new AccordionGame(), config);
    }
}
