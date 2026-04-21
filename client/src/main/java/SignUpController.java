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

public class SignUpController {

    private GuiClient app;

    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Label errorLabel;
    private Button signUpButton;

    public SignUpController(GuiClient app) {
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

        Label subtitleLabel = new Label("Create an account");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.getStyleClass().add("subtitle");

        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

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
        usernameField.setPromptText("Choose a username");
        usernameField.setPrefHeight(40);
        usernameField.getStyleClass().add("text-field");

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("System", 13));
        passwordLabel.getStyleClass().add("field-label");

        passwordField = new PasswordField();
        passwordField.setPromptText("Choose a password");
        passwordField.setPrefHeight(40);
        passwordField.getStyleClass().add("text-field");

        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Confirm Password field
        VBox confirmPasswordBox = new VBox(8);
        Label confirmPasswordLabel = new Label("Confirm Password");
        confirmPasswordLabel.setFont(Font.font("System", 13));
        confirmPasswordLabel.getStyleClass().add("field-label");

        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your password");
        confirmPasswordField.setPrefHeight(40);
        confirmPasswordField.getStyleClass().add("text-field");

        confirmPasswordBox.getChildren().addAll(confirmPasswordLabel, confirmPasswordField);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Sign Up button
        signUpButton = new Button("SIGN UP");
        signUpButton.setPrefHeight(45);
        signUpButton.setMaxWidth(Double.MAX_VALUE);
        signUpButton.getStyleClass().add("primary-button");
        signUpButton.setOnAction(e -> handleSignUp());

        // Make Enter key trigger sign up
        confirmPasswordField.setOnAction(e -> handleSignUp());

        formBox.getChildren().addAll(usernameBox, passwordBox, confirmPasswordBox, errorLabel, signUpButton);

        // Login link
        HBox loginBox = new HBox(5);
        loginBox.setAlignment(Pos.CENTER);

        Label loginPrompt = new Label("Already have an account?");
        loginPrompt.setFont(Font.font("System", 12));
        loginPrompt.getStyleClass().add("secondary-text");

        Button loginLink = new Button("Login");
        loginLink.getStyleClass().add("link-button");
        loginLink.setOnAction(e -> handleLogin());

        loginBox.getChildren().addAll(loginPrompt, loginLink);

        root.getChildren().addAll(titleBox, formBox, loginBox);

        Scene scene = new Scene(root, 400, 550);
        try {
            scene.getStylesheets().add(getClass().getResource("/login-styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load css");
            e.printStackTrace();
        }
        return scene;
    }

    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        System.out.println("Sign up attempt - Username: " + username);
        app.setUsername(username);
        Message msg = new Message(MessageType.REGISTER, username + ":" + password);
        app.send(msg);

        signUpButton.setDisable(true);
        signUpButton.setText("REGISTERING...");
    }

    private void handleLogin() {
        app.switchToScene("login");
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        signUpButton.setDisable(false);
        signUpButton.setText("SIGN UP");
    }
}
