import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    private ListView<String> friendsList;
    private Label welcomeLabel;
    private Label friendStatusLabel;

    private Label winsCountLabel;
    private Label lossesCountLabel;
    private Label drawsCountLabel;

    private Stage currentInviteStage;
    private boolean isWaitingInvite;

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

        // Friends section
        VBox friendsBox = new VBox(10);
        friendsBox.setPadding(new Insets(20, 0, 0, 0));
        VBox.setVgrow(friendsBox, Priority.ALWAYS);

        Label friendsLabel = new Label("Friends");
        friendsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        friendsLabel.setStyle("-fx-text-fill: #e6eef6;");

        // Add friend row
        HBox addFriendRow = new HBox(10);
        addFriendRow.setAlignment(Pos.CENTER_LEFT);

        TextField addFriendField = new TextField();
        addFriendField.setPromptText("Enter username...");
        addFriendField.setStyle(
                "-fx-background-color: #111419; -fx-text-fill: #e6eef6; -fx-prompt-text-fill: #555; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13;");
        HBox.setHgrow(addFriendField, Priority.ALWAYS);

        Button addFriendBtn = new Button("+ Add Friend");
        addFriendBtn.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        addFriendBtn.setOnAction(e -> {
            String friendName = addFriendField.getText().trim();
            if (!friendName.isEmpty()) {
                app.send(new Message(MessageType.ADD_FRIEND, friendName));
                addFriendField.clear();
            }
        });

        // Also allow pressing Enter to add
        addFriendField.setOnAction(e -> addFriendBtn.fire());

        addFriendRow.getChildren().addAll(addFriendField, addFriendBtn);

        // Status label for feedback
        friendStatusLabel = new Label();
        friendStatusLabel.setFont(Font.font("System", 12));
        friendStatusLabel.setStyle("-fx-text-fill: #9aa6b2;");

        friendsList = new ListView<>();
        friendsList.setStyle(
                "-fx-background-color: #111419; -fx-control-inner-background: #111419; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6; -fx-background-radius: 6;");
        VBox.setVgrow(friendsList, Priority.ALWAYS);

        // Cell factory: each item is "friendName:online" or "friendName:offline"
        friendsList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String[] parts = item.split(":");
                    String name = parts[0];
                    boolean isOnline = parts.length > 1 && parts[1].equals("online");

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

                    // Online/Offline dot
                    Circle statusDot = new Circle(4);
                    statusDot.setFill(isOnline ? Color.web("#2ecc71") : Color.web("#95a5a6"));

                    // Status text
                    Label statusText = new Label(isOnline ? "Online" : "Offline");
                    statusText.setFont(Font.font("System", 11));
                    statusText.setTextFill(isOnline ? Color.web("#2ecc71") : Color.web("#95a5a6"));

                    // Spacer
                    Region spacerObj = new Region();
                    HBox.setHgrow(spacerObj, Priority.ALWAYS);

                    row.getChildren().addAll(avatarPane, nameLabel, statusDot, statusText, spacerObj);

                    // Invite button (only for online friends)
                    if (isOnline) {
                        Button inviteBtn = new Button("Invite");
                        inviteBtn.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #ff2dd0; -fx-border-color: #ff2dd0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12; -fx-padding: 4 12; -fx-cursor: hand;");
                        inviteBtn.setOnAction(e -> {
                            System.out.println("Invite clicked for: " + name);
                            app.send(new Message(MessageType.MATCH_INVITE, name));
                            showWaitingForFriendModal(name);
                        });
                        row.getChildren().add(inviteBtn);
                    }

                    // Remove friend button
                    Button removeBtn = new Button("✕");
                    removeBtn.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12; -fx-padding: 4 8; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> {
                        app.send(new Message(MessageType.REMOVE_FRIEND, name));
                    });
                    row.getChildren().add(removeBtn);

                    setGraphic(row);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0; -fx-padding: 0;");
                }
            }
        });

        friendsBox.getChildren().addAll(friendsLabel, addFriendRow, friendStatusLabel, friendsList);

        centerContent.getChildren().addAll(welcomeLabel, statsRow, buttonsRow, friendsBox);

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

    /**
     * Called when the server sends a FRIENDS_LIST_UPDATE.
     * Content format: "friend1:online,friend2:offline,..."
     */
    public void onFriendsListUpdate(Message message) {
        friendsList.getItems().clear();
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String[] entries = message.getContent().split(",");
            for (String entry : entries) {
                friendsList.getItems().add(entry);
            }
        }
    }

    /**
     * Called when the server responds to an ADD_FRIEND request.
     * Content format: "SUCCESS:username" or "ERROR:message"
     */
    public void onAddFriendResponse(Message message) {
        if (message.getContent() != null) {
            if (message.getContent().startsWith("ERROR:")) {
                String error = message.getContent().substring(6);
                if (friendStatusLabel != null) {
                    friendStatusLabel.setText("⚠ " + error);
                    friendStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            } else if (message.getContent().startsWith("SUCCESS:")) {
                String friendName = message.getContent().substring(8);
                if (friendStatusLabel != null) {
                    friendStatusLabel.setText("✓ Added " + friendName + " as friend!");
                    friendStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
                }
            }
        }
    }

    /**
     * Show modal while waiting for friend to accept
     */
    private void showWaitingForFriendModal(String friendName) {
        Platform.runLater(() -> {
            if (currentInviteStage != null && currentInviteStage.isShowing()) {
                currentInviteStage.close();
            }

            isWaitingInvite = true;
            showCustomModal(
                "Inviting Friend",
                "Waiting for " + friendName + "...",
                true,
                "Cancel",
                null,
                () -> app.send(new Message(MessageType.MATCH_INVITE_CANCEL, friendName)),
                null
            );
        });
    }

    /**
     * Received a match invite from a friend.
     */
    public void onMatchInvite(Message message) {
        String sender = message.getContent();
        Platform.runLater(() -> {
            if (currentInviteStage != null && currentInviteStage.isShowing()) {
                currentInviteStage.close();
            }

            isWaitingInvite = false;
            showCustomModal(
                "Match Invite",
                sender + " invited you to a match! Accept?",
                false,
                "✓ Accept",
                "✗ Decline",
                () -> app.send(new Message(MessageType.MATCH_INVITE_ACCEPT, sender)),
                () -> app.send(new Message(MessageType.MATCH_INVITE_DECLINE, sender))
            );
        });
    }

    /**
     * Received when sender cancels their invite.
     */
    public void onMatchInviteCancel(Message message) {
        Platform.runLater(() -> {
            if (currentInviteStage != null && currentInviteStage.isShowing() && !isWaitingInvite) {
                currentInviteStage.close();
                currentInviteStage = null;
            }
        });
    }

    /**
     * Received response for sent invite (in_game, declined, offline, accepted).
     */
    public void onMatchInviteResponse(Message message) {
        String response = message.getContent();
        Platform.runLater(() -> {
            if (currentInviteStage != null && currentInviteStage.isShowing() && isWaitingInvite) {
                currentInviteStage.close();
                currentInviteStage = null;
            }

            if ("accepted".equals(response)) {
                // Game will start, no info modal needed
                return;
            }

            String messageText;
            if ("in_game".equals(response)) {
                messageText = "Friend is currently in a game.";
            } else if ("declined".equals(response)) {
                messageText = "Friend declined the invite.";
            } else if ("offline".equals(response)) {
                messageText = "Friend went offline.";
            } else {
                messageText = "Invite failed: " + response;
            }

            showCustomModal(
                "Invite Response",
                messageText,
                false,
                "OK",
                null,
                null,
                null
            );
        });
    }

    private void showCustomModal(String title, String subtitle, boolean isGoldIcon, String leftBtnText, String rightBtnText, Runnable onLeftClick, Runnable onRightClick) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        // Find main window
        if (friendsList != null && friendsList.getScene() != null) {
            dialogStage.initOwner(friendsList.getScene().getWindow());
        }
        
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox dialogRoot = new VBox(20);
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setPadding(new Insets(40, 50, 40, 50));
        dialogRoot.setStyle("-fx-background-color: #111419; -fx-background-radius: 12; -fx-border-color: rgba(0,240,255,0.3); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.2), 25, 0, 0, 0);");

        // Icon
        Label iconLabel = new Label(isGoldIcon ? "★" : "⚑");
        iconLabel.setStyle("-fx-font-size: 48; -fx-text-fill: #FFD166; -fx-background-color: #1a2030; -fx-background-radius: 50; -fx-padding: 20 28 20 28;");
        iconLabel.setAlignment(Pos.CENTER);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.web("#e6eef6"));

        // Subtitle
        Label subLabel = new Label(subtitle);
        subLabel.setFont(Font.font("System", 16));
        subLabel.setTextFill(Color.web("#9aa6b2"));

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        if (leftBtnText != null) {
            Button leftBtn = new Button(leftBtnText);
            leftBtn.setStyle("-fx-background-color: #00f0ff; -fx-text-fill: #0b0f14; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.3), 10, 0, 0, 0);");
            leftBtn.setOnAction(e -> {
                dialogStage.close();
                if (onLeftClick != null) onLeftClick.run();
            });
            buttonBox.getChildren().add(leftBtn);
        }

        if (rightBtnText != null) {
            Button rightBtn = new Button(rightBtnText);
            rightBtn.setStyle("-fx-background-color: #1a1f26; -fx-text-fill: #e6eef6; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8; -fx-border-width: 1;");
            rightBtn.setOnAction(e -> {
                dialogStage.close();
                if (onRightClick != null) onRightClick.run();
            });
            buttonBox.getChildren().add(rightBtn);
        }

        dialogRoot.getChildren().addAll(iconLabel, titleLabel, subLabel, buttonBox);

        Scene dialogScene = new Scene(dialogRoot);
        dialogScene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(dialogScene);
        
        currentInviteStage = dialogStage;
        dialogStage.showAndWait();
        currentInviteStage = null;
    }

    public void reset() {
        // Clear feedback label on re-entry
        if (friendStatusLabel != null) {
            friendStatusLabel.setText("");
        }
    }
}
