import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Message;
import model.Message.MessageType;

public class LobbyController {

    private GuiClient app;

    private String username = "Player";
    private ListView<String> onlinePlayersList;
    private Label welcomeLabel;

    private Label winsCountLabel;
    private Label lossesCountLabel;
    private Label drawsCountLabel;

    public LobbyController(GuiClient app) {
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

        Scene scene = new Scene(root, 1000, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/app-styles.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
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

        // Logout button
        Button logoutButton = new Button("\u2192 Logout");
        logoutButton.setOnAction(e -> handleLogout());
        logoutButton.setStyle(
                "-fx-background-color: #111419; -fx-text-fill: #9aa6b2; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6; -fx-border-width: 1;");

        topBar.getChildren().addAll(titleLabel, spacer, logoutButton);

        return topBar;
    }

    private VBox createStatCircle(String letter, String label, String count, String colorHex) {
        VBox statBox = new VBox(4);
        statBox.setAlignment(Pos.CENTER);

        // Circle with letter
        StackPane circlePane = new StackPane();
        Circle circle = new Circle(20);
        circle.setFill(Color.web(colorHex));

        Label letterLabel = new Label(letter);
        letterLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        letterLabel.setTextFill(Color.WHITE);

        circlePane.getChildren().addAll(circle, letterLabel);

        // Label text
        Label labelText = new Label(label);
        labelText.setFont(Font.font("System", 12));
        labelText.setTextFill(Color.web("#9aa6b2"));

        // Count
        Label countLabel = new Label(count);
        countLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        countLabel.setTextFill(Color.web("#e6eef6"));

        statBox.getChildren().addAll(circlePane, labelText, countLabel);
        return statBox;
    }

    private VBox createCenterContent() {
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(30, 40, 30, 40));
        centerContent.setAlignment(Pos.TOP_LEFT);

        // Welcome message
        welcomeLabel = new Label("Welcome, " + username);
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        welcomeLabel.setStyle("-fx-text-fill: #e6eef6;");

        // Stats circles row
        HBox statsRow = new HBox(25);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setPadding(new Insets(5, 0, 15, 0));

        VBox winsBox = createStatCircle("W", "Wins", "0", "#2ecc71");
        VBox lossesBox = createStatCircle("L", "Losses", "0", "#e74c3c");
        VBox drawsBox = createStatCircle("D", "Draws", "0", "#95a5a6");

        // Store count labels for updating
        winsCountLabel = (Label) winsBox.getChildren().get(2);
        lossesCountLabel = (Label) lossesBox.getChildren().get(2);
        drawsCountLabel = (Label) drawsBox.getChildren().get(2);

        statsRow.getChildren().addAll(winsBox, lossesBox, drawsBox);

        // Game buttons - horizontal row
        HBox buttonsRow = new HBox(15);
        buttonsRow.setAlignment(Pos.CENTER);

        String buttonStyle = "-fx-background-color: #00f0ff; -fx-text-fill: #0b0f14; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.3), 10, 0, 0, 0);";

        Button playBotButton = new Button("PLAY VS BOT");
        playBotButton.setPrefHeight(60);
        playBotButton.setMaxWidth(Double.MAX_VALUE);
        playBotButton.setOnAction(e -> handleQuickPlay());
        playBotButton.setStyle(buttonStyle);
        HBox.setHgrow(playBotButton, Priority.ALWAYS);

        Button playFriendButton = new Button("PLAY LOCAL VS FRIEND");
        playFriendButton.setPrefHeight(60);
        playFriendButton.setMaxWidth(Double.MAX_VALUE);
        playFriendButton.setOnAction(e -> handlePlayWithFriend());
        playFriendButton.setStyle(buttonStyle);
        HBox.setHgrow(playFriendButton, Priority.ALWAYS);

        Button createGameButton = new Button("PLAY ONLINE VS FRIEND");
        createGameButton.setPrefHeight(60);
        createGameButton.setMaxWidth(Double.MAX_VALUE);
        createGameButton.setOnAction(e -> handleCreateGame());
        createGameButton.setStyle(buttonStyle);
        HBox.setHgrow(createGameButton, Priority.ALWAYS);

        buttonsRow.getChildren().addAll(playBotButton, playFriendButton, createGameButton);

        // Online Players section
        VBox onlinePlayersBox = new VBox(10);
        onlinePlayersBox.setPadding(new Insets(20, 0, 0, 0));
        VBox.setVgrow(onlinePlayersBox, Priority.ALWAYS);

        Label onlinePlayersLabel = new Label("Online Players");
        onlinePlayersLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        onlinePlayersLabel.setStyle("-fx-text-fill: #e6eef6;");

        onlinePlayersList = new ListView<>();
        onlinePlayersList.setStyle(
                "-fx-background-color: #111419; -fx-control-inner-background: #111419; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6; -fx-background-radius: 6;");
        VBox.setVgrow(onlinePlayersList, Priority.ALWAYS);

        // List starts empty — populated by ONLINE_PLAYERS_UPDATE from server

        onlinePlayersList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String name = item;

                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10, 15, 10, 15));

                    // Avatar circle with first letter
                    StackPane avatarPane = new StackPane();
                    Circle avatar = new Circle(18);
                    avatar.setFill(Color.web("#1a3a4a"));

                    Label avatarLetter = new Label(name.substring(0, 1).toUpperCase());
                    avatarLetter.setFont(Font.font("System", FontWeight.BOLD, 14));
                    avatarLetter.setTextFill(Color.WHITE);

                    avatarPane.getChildren().addAll(avatar, avatarLetter);

                    // Name
                    Label nameLabel = new Label(name);
                    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    nameLabel.setTextFill(Color.web("#e6eef6"));

                    // Online dot (always green — all players in list are online)
                    Circle statusDot = new Circle(4);
                    statusDot.setFill(Color.web("#2ecc71"));

                    // Spacer
                    Region spacerObj = new Region();
                    HBox.setHgrow(spacerObj, Priority.ALWAYS);

                    row.getChildren().addAll(avatarPane, nameLabel, statusDot, spacerObj);

                    // Invite button
                    Button inviteBtn = new Button("Invite");
                    inviteBtn.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: #ff2dd0; -fx-border-color: #ff2dd0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12; -fx-padding: 4 12; -fx-cursor: hand;");
                    inviteBtn.setOnAction(e -> {
                        System.out.println("Invite clicked for: " + name);
                    });
                    row.getChildren().add(inviteBtn);

                    setGraphic(row);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0; -fx-padding: 0;");
                }
            }
        });

        onlinePlayersBox.getChildren().addAll(onlinePlayersLabel, onlinePlayersList);

        centerContent.getChildren().addAll(welcomeLabel, statsRow, buttonsRow, onlinePlayersBox);

        return centerContent;
    }

    private void handleQuickPlay() {
        System.out.println("Quick Play clicked - Starting game vs AI");
        app.send(new Message(MessageType.QUEUE, "AI"));
    }

    private void handlePlayWithFriend() {
        System.out.println("Play Local with Friend clicked - Starting local hot-seat game");
        app.send(new Message(MessageType.QUEUE, "LOCAL"));
    }

    private void handleCreateGame() {
        System.out.println("Play Online vs Friend clicked - Starting PVP Matchmaking");
        app.send(new Message(MessageType.QUEUE, "PVP"));
        app.switchToScene("matchmaking");
    }

    private void handleLogout() {
        System.out.println("Logout clicked");
        app.switchToScene("login");
    }

    public void onAuthSuccess(Message message) {
        this.username = app.getUsername();
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username);
        }

        // Parse stats if attached
        if (message.getContent() != null && message.getContent().startsWith("Login successful:")) {
            String[] parts = message.getContent().split(":");
            if (parts.length == 4) {
                if (winsCountLabel != null) winsCountLabel.setText(parts[1]);
                if (lossesCountLabel != null) lossesCountLabel.setText(parts[2]);
                if (drawsCountLabel != null) drawsCountLabel.setText(parts[3]);
            }
        }
    }

    public void onStatsUpdate(Message message) {
        if (message.getContent() != null) {
            String[] parts = message.getContent().split(":");
            if (parts.length == 3) {
                if (winsCountLabel != null) winsCountLabel.setText(parts[0]);
                if (lossesCountLabel != null) lossesCountLabel.setText(parts[1]);
                if (drawsCountLabel != null) drawsCountLabel.setText(parts[2]);
            }
        }
    }

    public void onOnlinePlayersUpdate(Message message) {
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String[] players = message.getContent().split(",");
            onlinePlayersList.getItems().clear();
            for (String player : players) {
                // Don't show yourself in the online list
                if (!player.equals(username)) {
                    onlinePlayersList.getItems().add(player);
                }
            }
        } else {
            onlinePlayersList.getItems().clear();
        }
    }

    public void reset() {
        // Reset lobby state if necessary
    }
}
