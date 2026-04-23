import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.Message;

public class MatchmakingController {

    // Fields for the matchmaking screen
    private GuiClient app;
    private String username = "Player";
    private Label statusLabel;
    private Label timerLabel;
    private Label usernameLabel;
    private int elapsedSeconds = 0;
    private Timeline timer;

    public MatchmakingController(GuiClient app) {
        this.app = app;
    }

    // Create the matchmaking scene
    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Center content
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 600, 500);
        try {
            scene.getStylesheets().add(getClass().getResource("/app-styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles in MatchmakingController: " + e.getMessage());
        }

        return scene;
    }

    // Create the top bar with title and username
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(20, 30, 20, 30));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        // Title
        Label titleLabel = new Label("CHECKERS");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User info
        usernameLabel = new Label(username);
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        usernameLabel.getStyleClass().add("username-label");

        topBar.getChildren().addAll(titleLabel, spacer, usernameLabel);

        return topBar;
    }

    // Create the center content with the matchmaking container
    private VBox createCenterContent() {
        VBox centerContent = new VBox(30);
        centerContent.setPadding(new Insets(60, 40, 40, 40));
        centerContent.setAlignment(Pos.CENTER);

        // Matchmaking container
        VBox matchmakingBox = new VBox(25);
        matchmakingBox.setAlignment(Pos.CENTER);
        matchmakingBox.setPadding(new Insets(50, 60, 50, 60));
        matchmakingBox.getStyleClass().add("form-container");
        matchmakingBox.setMaxWidth(450);

        // Status label
        statusLabel = new Label("Searching for opponent...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        statusLabel.getStyleClass().add("title");

        // Progress indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.getStyleClass().add("progress-indicator");
        progressIndicator.setPrefSize(80, 80);

        // Timer label
        timerLabel = new Label("0:00");
        timerLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        timerLabel.getStyleClass().add("subtitle");

        // Info label
        Label infoLabel = new Label("Please wait while we find you a match");
        infoLabel.setFont(Font.font("System", 14));
        infoLabel.getStyleClass().add("secondary-text");

        // Players searching info
        VBox playersInfoBox = new VBox(8);
        playersInfoBox.setAlignment(Pos.CENTER);

        Label estimatedTimeLabel = new Label("Estimated wait: < 1 min");
        estimatedTimeLabel.setFont(Font.font("System", 13));
        estimatedTimeLabel.getStyleClass().add("secondary-text");

        playersInfoBox.getChildren().addAll(estimatedTimeLabel);

        // Cancel button
        Button cancelButton = new Button("CANCEL SEARCH");
        cancelButton.setPrefHeight(50);
        cancelButton.setPrefWidth(250);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> handleCancel());

        // Add the status label, progress indicator, info label, players info box, and cancel button to the matchmaking box
        matchmakingBox.getChildren().addAll(
            statusLabel,
            progressIndicator,
            infoLabel,
            playersInfoBox,
            cancelButton
        );

        centerContent.getChildren().add(matchmakingBox);

        return centerContent;
    }

    // Timeline for queue delay
    private Timeline queueDelay;

    // Start the timer
    public void startTimer() {
        elapsedSeconds = 0;
        if (timerLabel != null) timerLabel.setText("0:00");
        if (timer != null) timer.stop();
        
        // Create a new timeline for the timer
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            int minutes = elapsedSeconds / 60;
            int seconds = elapsedSeconds % 60;
            if (timerLabel != null) {
                timerLabel.setText(String.format("%d:%02d", minutes, seconds));
            }
        }));
        // Set the timer to run indefinitely
        timer.setCycleCount(Timeline.INDEFINITE);
        // Start the timer
        timer.play();
    }

    // Stop the timer
    public void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
        if (queueDelay != null) {
            queueDelay.stop();
        }
    }

    // Queue for a match
    public void queueForMatch(String mode) {
        if (queueDelay != null) queueDelay.stop();
        
        queueDelay = new Timeline(new KeyFrame(Duration.seconds(2.0), e -> {
            app.send(new Message(Message.MessageType.QUEUE, mode)); // sending network queue request cleanly
        }));
        queueDelay.play();
    }

    // Handle cancel button
    private void handleCancel() {
        stopTimer();
        app.send(new Message(Message.MessageType.LEAVE_QUEUE, ""));
        System.out.println("Matchmaking cancelled - Returning to lobby");
        app.switchToScene("lobby");
    }

    // Set the username
    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) usernameLabel.setText(username);
    }
}
