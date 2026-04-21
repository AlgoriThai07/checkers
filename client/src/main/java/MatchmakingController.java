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

public class MatchmakingController {

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

        Label playersCountLabel = new Label("Players searching: 12");
        playersCountLabel.setFont(Font.font("System", 13));
        playersCountLabel.getStyleClass().add("secondary-text");

        Label estimatedTimeLabel = new Label("Estimated wait: < 1 min");
        estimatedTimeLabel.setFont(Font.font("System", 13));
        estimatedTimeLabel.getStyleClass().add("secondary-text");

        playersInfoBox.getChildren().addAll(playersCountLabel, estimatedTimeLabel);

        // Cancel button
        Button cancelButton = new Button("CANCEL SEARCH");
        cancelButton.setPrefHeight(50);
        cancelButton.setPrefWidth(250);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> handleCancel());

        matchmakingBox.getChildren().addAll(
            statusLabel,
            progressIndicator,
            timerLabel,
            infoLabel,
            playersInfoBox,
            cancelButton
        );

        centerContent.getChildren().add(matchmakingBox);

        return centerContent;
    }

    private Timeline queueDelay;

    public void startTimer() {
        elapsedSeconds = 0;
        if (timerLabel != null) timerLabel.setText("0:00");
        if (timer != null) timer.stop();
        
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            int minutes = elapsedSeconds / 60;
            int seconds = elapsedSeconds % 60;
            if (timerLabel != null) {
                timerLabel.setText(String.format("%d:%02d", minutes, seconds));
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
        if (queueDelay != null) {
            queueDelay.stop();
        }
    }

    public void queueForMatch(String mode) {
        if (queueDelay != null) queueDelay.stop();
        
        queueDelay = new Timeline(new KeyFrame(Duration.seconds(2.0), e -> {
            app.send(new model.Message(model.Message.MessageType.QUEUE, mode)); // sending network queue request cleanly
        }));
        queueDelay.play();
    }

    private void handleCancel() {
        stopTimer();
        System.out.println("Matchmaking cancelled - Returning to lobby");
        app.switchToScene("lobby");
    }

    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) usernameLabel.setText(username);
    }
}
