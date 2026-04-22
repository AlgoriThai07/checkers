import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private ListView<String> onlinePlayersList;
    private TextField searchField;
    private Label welcomeLabel;

    private Label winsLabel;
    private Label lossesLabel;
    private Label drawsLabel;

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

        // Right panel (Friends)
        VBox friendsPanel = createFriendsPanel();
        root.setRight(friendsPanel);

        Scene scene = new Scene(root, 900, 600);
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
        Label titleLabel = new Label("CHECKERS ONLINE");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Logout button
        Button logoutButton = new Button("[\u2192 Logout");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(e -> handleLogout());
        // Match the image style EXACTLY
        logoutButton.setStyle(
                "-fx-background-color: #383431; -fx-text-fill: #e8e6e3; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");

        topBar.getChildren().addAll(titleLabel, spacer, logoutButton);

        return topBar;
    }

    private Label createStatBadge(String text, String colorHex) {
        Label badge = new Label(text);
        badge.setFont(Font.font("System", FontWeight.BOLD, 16));
        badge.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: #151311; -fx-padding: 6 15 6 15; -fx-background-radius: 12;");
        return badge;
    }

    private VBox createCenterContent() {
        VBox centerContent = new VBox(30);
        centerContent.setPadding(new Insets(40, 40, 40, 40));
        centerContent.setAlignment(Pos.CENTER);

        // Welcome message
        welcomeLabel = new Label("Welcome, " + username + "!");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        welcomeLabel.getStyleClass().add("welcome-label");
        welcomeLabel.setStyle("-fx-text-fill: #e8e6e3;");

        // Stats Banner
        HBox statsBanner = new HBox(15);
        statsBanner.setAlignment(Pos.CENTER);
        statsBanner.setPadding(new Insets(10, 0, 20, 0));

        winsLabel = createStatBadge("0 W", "#81b64c");    // Lime Green
        lossesLabel = createStatBadge("0 L", "#e05252");  // Soft Red
        drawsLabel = createStatBadge("0 D", "#a29f9c");   // Grey

        statsBanner.getChildren().addAll(winsLabel, lossesLabel, drawsLabel);

        // Game options container
        VBox gameOptionsBox = new VBox(20);
        gameOptionsBox.setAlignment(Pos.CENTER);
        gameOptionsBox.setPadding(new Insets(30, 40, 30, 40));
        gameOptionsBox.getStyleClass().add("form-container"); // Reuse form container styling
        gameOptionsBox.setMaxWidth(500);

        // Play buttons
        Button quickPlayButton = new Button("PLAY VS BOT");
        quickPlayButton.setPrefHeight(60);
        quickPlayButton.setMaxWidth(Double.MAX_VALUE);
        quickPlayButton.getStyleClass().add("primary-button");
        quickPlayButton.setOnAction(e -> handleQuickPlay());

        Button playLocalButton = new Button("PLAY LOCAL VS FRIEND");
        playLocalButton.setPrefHeight(60);
        playLocalButton.setMaxWidth(Double.MAX_VALUE);
        playLocalButton.getStyleClass().add("primary-button");
        playLocalButton.setOnAction(e -> handleCreateGame()); // Assuming this will be hooked up later

        Button playOnlineButton = new Button("PLAY ONLINE VS FRIEND");
        playOnlineButton.setPrefHeight(60);
        playOnlineButton.setMaxWidth(Double.MAX_VALUE);
        playOnlineButton.getStyleClass().add("primary-button");
        playOnlineButton.setOnAction(e -> handlePlayWithFriend());

        gameOptionsBox.getChildren().addAll(quickPlayButton, playLocalButton, playOnlineButton);

        centerContent.getChildren().addAll(welcomeLabel, statsBanner, gameOptionsBox);

        // Online players section
        VBox onlinePlayersBox = new VBox(15);
        onlinePlayersBox.setAlignment(Pos.CENTER);
        onlinePlayersBox.setMaxWidth(500);

        Label onlinePlayersLabel = new Label("Online Players");
        onlinePlayersLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        onlinePlayersLabel.getStyleClass().add("section-label");
        onlinePlayersLabel.setStyle("-fx-text-fill: #c7c4c0;");

        searchField = new TextField();
        searchField.setPromptText("Search players...");
        searchField.setPrefHeight(35);
        searchField.getStyleClass().add("text-field");

        onlinePlayersList = new ListView<>();
        onlinePlayersList.setPrefHeight(150);
        onlinePlayersList.getStyleClass().add("players-list");
        onlinePlayersList.setStyle(
                "-fx-background-color: #1a1613; -fx-control-inner-background: #1a1613; -fx-text-fill: #e8e6e3; -fx-border-color: #3d3935;");

        // Sample data
        onlinePlayersList.getItems().addAll(
                "Alice (Online)",
                "Bob (In Game)",
                "Charlie (Online)",
                "Diana (Online)",
                "Eve (In Game)");

        onlinePlayersBox.getChildren().addAll(onlinePlayersLabel, searchField, onlinePlayersList);

        // Remove the old direct addition of welcomeLabel and gameOptionsBox since we did it above
        centerContent.getChildren().addAll(onlinePlayersBox);

        return centerContent;
    }

    private VBox createFriendsPanel() {
        VBox friendsPanel = new VBox(15);
        friendsPanel.setPadding(new Insets(20, 20, 20, 20));
        friendsPanel.getStyleClass().add("friends-panel");
        friendsPanel.setPrefWidth(280);
        friendsPanel.setStyle("-fx-background-color: #262421; -fx-border-color: #3d3935; -fx-border-width: 0 0 0 1;");

        // Friends header
        HBox friendsHeader = new HBox(10);
        friendsHeader.setAlignment(Pos.CENTER_LEFT);
        friendsHeader.setPadding(new Insets(15, 10, 15, 10));
        friendsHeader.setStyle("-fx-border-color: #3d3935; -fx-border-width: 0 0 1 0;");

        Label friendsLabel = new Label("FRIENDS");
        friendsLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        friendsLabel.setStyle("-fx-text-fill: #968e85;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addFriendButton = new Button("+");
        addFriendButton.getStyleClass().add("add-friend-button");
        addFriendButton.setOnAction(e -> handleAddFriend());
        addFriendButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #1a1613; -fx-font-weight: bold; -fx-background-radius: 15; -fx-pref-width: 0; -fx-opacity: 0;"); // Hidden
                                                                                                                                                                    // to
                                                                                                                                                                    // match
                                                                                                                                                                    // image

        friendsHeader.getChildren().addAll(friendsLabel, spacer, addFriendButton);

        // Friends list
        friendsList = new ListView<>();
        friendsList.getStyleClass().add("friends-list");
        friendsList.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-text-fill: #e8e6e3; -fx-padding: 0;");
        VBox.setVgrow(friendsList, Priority.ALWAYS);

        // Sample friends data
        friendsList.getItems().addAll(
                "Alice:Online",
                "Bob:Online",
                "Diana:Online",
                "George:Online",
                "Charlie:Offline",
                "Edward:Offline",
                "Fiona:Offline");

        friendsList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                } else {
                    String[] parts = item.split(":");
                    String name = parts[0];
                    boolean isOnline = parts.length > 1 && parts[1].equals("Online");

                    HBox row = new HBox(15);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(12, 10, 12, 10));

                    Circle statusDot = new Circle(5);
                    statusDot.setFill(isOnline ? Color.web("#80C94D") : Color.web("#666666"));

                    Label nameLabel = new Label(name);
                    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    nameLabel.setTextFill(isOnline ? Color.web("#ffffff") : Color.web("#888888"));

                    Region spacerObj = new Region();
                    HBox.setHgrow(spacerObj, Priority.ALWAYS);

                    Label statusLabelObj = new Label(isOnline ? "Online" : "Offline");
                    statusLabelObj.setFont(Font.font("System", 13));
                    statusLabelObj.setTextFill(Color.web("#666666"));

                    row.getChildren().addAll(statusDot, nameLabel, spacerObj, statusLabelObj);

                    setGraphic(row);
                    setText(null);
                    setStyle(
                            "-fx-background-color: transparent; -fx-border-color: #3d3935; -fx-border-width: 0 0 1 0; -fx-padding: 0;");
                }
            }
        });

        friendsPanel.getChildren().addAll(friendsHeader, friendsList);

        return friendsPanel;
    }

    private void handleQuickPlay() {
        System.out.println("Quick Play clicked - Starting game vs AI");
        app.send(new Message(MessageType.QUEUE, "AI"));
    }

    private void handlePlayWithFriend() {
        System.out.println("Play with Friend clicked - Starting PVP Matchmaking");
        app.send(new Message(MessageType.QUEUE, "PVP"));
        app.switchToScene("matchmaking");
    }

    private void handleCreateGame() {
        System.out.println("Create Game clicked - UI Button pressed");
    }

    private void handleLogout() {
        System.out.println("Logout clicked");
        app.switchToScene("login");
    }

    private void handleAddFriend() {
        System.out.println("Add Friend clicked");
    }



    public void onAuthSuccess(Message message) {
        this.username = app.getUsername();
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username + "!");
        }

        // Parse stats if attached
        if (message.getContent() != null && message.getContent().startsWith("Login successful:")) {
            String[] parts = message.getContent().split(":");
            if (parts.length == 4) {
                if (winsLabel != null) winsLabel.setText(parts[1] + " W");
                if (lossesLabel != null) lossesLabel.setText(parts[2] + " L");
                if (drawsLabel != null) drawsLabel.setText(parts[3] + " D");
            }
        }
    }

    public void reset() {
        // Reset lobby state if necessary
    }
}
