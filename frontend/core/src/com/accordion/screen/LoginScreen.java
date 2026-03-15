package com.accordion.screen;

import com.accordion.AccordionGame;
import com.accordion.config.AppConfig;
import com.accordion.service.ApiClient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginScreen implements Screen {
    private static final Logger LOGGER = Logger.getLogger(LoginScreen.class.getName());
    
    private final AccordionGame game;
    private Stage stage;
    private TextField usernameField;
    private TextField passwordField;
    private Label errorLabel;
    private TextButton submitButton;
    private TextButton toggleButton;
    private Label toggleText;
    private Skin skin;
    private boolean isLoginMode = true;

    public LoginScreen(final AccordionGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Create UI elements
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Accordion Chat", skin);
        titleLabel.setFontScale(2);

        Label usernameLabel = new Label("Username:", skin);
        usernameField = new TextField("", skin);
        usernameField.setMessageText("Your username");

        Label passwordLabel = new Label("Password:", skin);
        passwordField = new TextField("", skin);
        passwordField.setMessageText("Your password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        submitButton = new TextButton("Login", skin);
        
        errorLabel = new Label("", skin);
        errorLabel.setColor(Color.RED);
        errorLabel.setWrap(true);

        toggleText = new Label("Don't have an account?", skin);
        toggleButton = new TextButton("Register", skin);

        // Add listeners
        submitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleSubmit();
            }
        });

        toggleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleMode();
            }
        });

        // Layout
        table.add(titleLabel).colspan(2).padBottom(40);
        table.row();
        table.add(usernameLabel).padRight(10).right();
        table.add(usernameField).width(250).padBottom(10);
        table.row();
        table.add(passwordLabel).padRight(10).right();
        table.add(passwordField).width(250).padBottom(20);
        table.row();
        table.add(submitButton).colspan(2).width(150).padBottom(20);
        table.row();
        table.add(errorLabel).colspan(2).width(350).padBottom(10);
        table.row();
        
        Table toggleTable = new Table();
        toggleTable.add(toggleText).padRight(10);
        toggleTable.add(toggleButton);
        table.add(toggleTable).colspan(2);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        errorLabel.setText("");
        if (isLoginMode) {
            submitButton.setText("Login");
            toggleText.setText("Don't have an account?");
            toggleButton.setText("Register");
        } else {
            submitButton.setText("Register");
            toggleText.setText("Already have an account?");
            toggleButton.setText("Login");
        }
    }

    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Validate username
        if (username.isEmpty()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        if (username.length() < AppConfig.MIN_USERNAME_LENGTH) {
            errorLabel.setText("Username must be at least " + AppConfig.MIN_USERNAME_LENGTH + " characters");
            return;
        }
        if (username.length() > AppConfig.MAX_USERNAME_LENGTH) {
            errorLabel.setText("Username must be " + AppConfig.MAX_USERNAME_LENGTH + " characters or less");
            return;
        }
        if (!username.matches(AppConfig.USERNAME_PATTERN)) {
            errorLabel.setText("Username can only contain letters, numbers, and underscores");
            return;
        }
        
        // Validate password
        if (password.isEmpty()) {
            errorLabel.setText("Password cannot be empty");
            return;
        }
        if (password.length() < AppConfig.MIN_PASSWORD_LENGTH) {
            errorLabel.setText("Password must be at least " + AppConfig.MIN_PASSWORD_LENGTH + " characters");
            return;
        }
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) {
            errorLabel.setText("Password must contain uppercase, lowercase, and a digit");
            return;
        }
        
        // Disable button while processing
        submitButton.setDisabled(true);
        submitButton.setText(isLoginMode ? "Logging in..." : "Registering...");
        errorLabel.setText("");
        
        // Make API call on background thread
        final String finalUsername = username;
        final String finalPassword = password;
        final boolean loginMode = isLoginMode;
        
        new Thread(() -> {
            try {
                ApiClient.AuthResponse response;
                if (loginMode) {
                    response = game.apiClient.login(finalUsername, finalPassword);
                } else {
                    response = game.apiClient.register(finalUsername, finalPassword);
                }
                
                final String authUsername = response.username;
                Gdx.app.postRunnable(() -> {
                    game.username = authUsername;
                    game.setScreen(new ChatScreen(game, authUsername));
                });
            } catch (ApiClient.ApiException e) {
                LOGGER.log(Level.WARNING, "Authentication failed", e);
                Gdx.app.postRunnable(() -> {
                    errorLabel.setText(e.getMessage());
                    submitButton.setDisabled(false);
                    submitButton.setText(loginMode ? "Login" : "Register");
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during authentication", e);
                Gdx.app.postRunnable(() -> {
                    errorLabel.setText("An unexpected error occurred. Please try again.");
                    submitButton.setDisabled(false);
                    submitButton.setText(loginMode ? "Login" : "Register");
                });
            }
        }).start();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
