package com.accordion.screen;

import com.accordion.AccordionGame;
import com.accordion.config.AppConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LoginScreen implements Screen {
    private final AccordionGame game;
    private Stage stage;
    private TextField usernameField;
    private Label errorLabel;
    private Skin skin;

    public LoginScreen(final AccordionGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Create a simple skin
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Create UI elements
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Accordion Chat", skin);
        titleLabel.setFontScale(2);

        Label usernameLabel = new Label("Enter Username:", skin);
        
        usernameField = new TextField("", skin);
        usernameField.setMessageText("Your username");

        TextButton loginButton = new TextButton("Login", skin);
        
        errorLabel = new Label("", skin);
        errorLabel.setColor(Color.RED);

        // Add listeners
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogin();
            }
        });

        // Layout
        table.add(titleLabel).colspan(2).padBottom(40);
        table.row();
        table.add(usernameLabel).padRight(10);
        table.add(usernameField).width(200).padBottom(20);
        table.row();
        table.add(loginButton).colspan(2).width(150).padBottom(20);
        table.row();
        table.add(errorLabel).colspan(2);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        
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
        
        // Validate allowed characters (alphanumeric and underscore only)
        if (!username.matches(AppConfig.USERNAME_PATTERN)) {
            errorLabel.setText("Username can only contain letters, numbers, and underscores");
            return;
        }
        
        // Navigate to chat screen
        game.setScreen(new ChatScreen(game, username));
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
