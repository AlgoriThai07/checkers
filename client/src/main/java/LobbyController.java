import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import model.Message;
import model.Message.MessageType;

public class LobbyController {

    private GuiClient app;

    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;

    private Button pvpButton;
    private Button aiButton;
    private Label statusLabel;

    private VBox loginBox;
    private VBox lobbyBox;

    private boolean loggedIn = false;

    public LobbyController(GuiClient app) {
        this.app = app;
    }

    public Scene createScene() {
        // ---- Login Section ----
        Label titleLabel = new Label("\u265B CHECKERS \u265B");
        titleLabel.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: #e0c068;");

        Label loginLabel = new Label("Sign In to Play");
        loginLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #cccccc;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(280);
        usernameField.setStyle("-fx-font-size: 14; -fx-background-color: #3c3c3c; -fx-text-fill: white; " +
                               "-fx-prompt-text-fill: #888888; -fx-background-radius: 5;");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(280);
        passwordField.setStyle("-fx-font-size: 14; -fx-background-color: #3c3c3c; -fx-text-fill: white; " +
                               "-fx-prompt-text-fill: #888888; -fx-background-radius: 5;");

        loginButton = new Button("Login");
        loginButton.setStyle("-fx-font-size: 14; -fx-background-color: #4a90d9; -fx-text-fill: white; " +
                             "-fx-background-radius: 5; -fx-padding: 8 30;");

        registerButton = new Button("Register");
        registerButton.setStyle("-fx-font-size: 14; -fx-background-color: #5a5a5a; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 8 30;");

        HBox authButtons = new HBox(15, loginButton, registerButton);
        authButtons.setAlignment(Pos.CENTER);

        loginBox = new VBox(15, titleLabel, loginLabel, usernameField, passwordField, authButtons);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));

        // ---- Lobby Section (hidden initially) ----
        Label welcomeLabel = new Label("Choose Game Mode");
        welcomeLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #e0c068;");

        pvpButton = new Button("Play vs Human");
        pvpButton.setStyle("-fx-font-size: 18; -fx-background-color: #c0392b; -fx-text-fill: white; " +
                           "-fx-background-radius: 8; -fx-padding: 15 40; -fx-cursor: hand;");
        pvpButton.setPrefWidth(280);

        aiButton = new Button("Play vs AI");
        aiButton.setStyle("-fx-font-size: 18; -fx-background-color: #2980b9; -fx-text-fill: white; " +
                          "-fx-background-radius: 8; -fx-padding: 15 40; -fx-cursor: hand;");
        aiButton.setPrefWidth(280);

        lobbyBox = new VBox(20, welcomeLabel, pvpButton, aiButton);
        lobbyBox.setAlignment(Pos.CENTER);
        lobbyBox.setPadding(new Insets(40));
        lobbyBox.setVisible(false);
        lobbyBox.setManaged(false);

        // ---- Status label ----
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #f39c12;");

        VBox root = new VBox(20, loginBox, lobbyBox, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPadding(new Insets(30));

        // ---- Button Actions ----
        loginButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (!user.isEmpty() && !pass.isEmpty()) {
                app.setUsername(user);
                Message msg = new Message(MessageType.LOGIN, user + ":" + pass);
                app.send(msg);
                statusLabel.setText("Logging in...");
            } else {
                statusLabel.setText("Please enter username and password");
            }
        });

        registerButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (!user.isEmpty() && !pass.isEmpty()) {
                Message msg = new Message(MessageType.REGISTER, user + ":" + pass);
                app.send(msg);
                statusLabel.setText("Registering...");
            } else {
                statusLabel.setText("Please enter username and password");
            }
        });

        pvpButton.setOnAction(e -> {
            Message msg = new Message(MessageType.QUEUE, "PVP");
            app.send(msg);
            statusLabel.setText("Waiting for opponent...");
            pvpButton.setDisable(true);
            aiButton.setDisable(true);
        });

        aiButton.setOnAction(e -> {
            Message msg = new Message(MessageType.QUEUE, "AI");
            app.send(msg);
            statusLabel.setText("Starting AI game...");
            pvpButton.setDisable(true);
            aiButton.setDisable(true);
        });

        return new Scene(root, 500, 550);
    }

    public void onAuthSuccess(Message message) {
        loggedIn = true;
        statusLabel.setText("Logged in as " + app.getUsername());

        loginBox.setVisible(false);
        loginBox.setManaged(false);
        lobbyBox.setVisible(true);
        lobbyBox.setManaged(true);
    }

    public void onAuthFail(Message message) {
        statusLabel.setText("Auth failed: " + message.getContent());
    }

    public void reset() {
        statusLabel.setText("");
        if (loggedIn) {
            pvpButton.setDisable(false);
            aiButton.setDisable(false);
        }
    }
}
