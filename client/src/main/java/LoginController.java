import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Message;
import model.Message.MessageType;

public class LoginController {

    private GuiClient app;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;
    private Button signupLink;

    public LoginController(GuiClient app) {
        this.app = app;
    }

    public Scene createScene() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("root");

        // Title
        Label titleLabel = new Label("CHECKERS");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.getStyleClass().add("title");

        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel);

        // Form container
        VBox formBox = new VBox(20);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(30, 40, 30, 40));
        formBox.getStyleClass().add("form-container");
        formBox.setMaxWidth(350);

        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", 13));
        usernameLabel.getStyleClass().add("field-label");

        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefHeight(40);
        usernameField.getStyleClass().add("text-field");

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("System", 13));
        passwordLabel.getStyleClass().add("field-label");

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(40);
        passwordField.getStyleClass().add("text-field");

        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Login button
        loginButton = new Button("LOGIN");
        loginButton.setPrefHeight(45);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.getStyleClass().add("primary-button");
        loginButton.setOnAction(e -> handleLogin());

        // Make Enter key trigger login
        passwordField.setOnAction(e -> handleLogin());

        formBox.getChildren().addAll(usernameBox, passwordBox, errorLabel, loginButton);

        // Sign up link
        HBox signupBox = new HBox(5);
        signupBox.setAlignment(Pos.CENTER);

        Label signupPrompt = new Label("Don't have an account?");
        signupPrompt.setFont(Font.font("System", 12));
        signupPrompt.getStyleClass().add("secondary-text");

        signupLink = new Button("Sign Up");
        signupLink.getStyleClass().add("link-button");
        signupLink.setOnAction(e -> handleSignUp());

        signupBox.getChildren().addAll(signupPrompt, signupLink);

        root.getChildren().addAll(titleBox, formBox, signupBox);

        Scene scene = new Scene(root, 400, 500);
        try {
            scene.getStylesheets().add(getClass().getResource("/app-styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load app-styles.css");
            e.printStackTrace();
        }
        return scene;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }

        System.out.println("Login attempt - Username: " + username);
        // Connect to server for authentication
        app.setUsername(username);
        Message msg = new Message(MessageType.LOGIN, username + ":" + password);
        app.send(msg);

        // Disable buttons while loading
        loginButton.setDisable(true);
        loginButton.setText("LOGGING IN...");
    }

    private void handleSignUp() {
        app.switchToScene("signup");
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        loginButton.setDisable(false);
        loginButton.setText("LOGIN");
    }

    public void reset() {
        loginButton.setDisable(false);
        loginButton.setText("LOGIN");
        passwordField.clear();
        usernameField.clear();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
