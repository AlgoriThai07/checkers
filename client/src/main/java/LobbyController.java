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
        Label titleLabel = new Label("CHECKERS");
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

    private VBox createCenterContent() {
        VBox centerContent = new VBox(30);
        centerContent.setPadding(new Insets(40, 40, 40, 40));
        centerContent.setAlignment(Pos.CENTER);

        // Welcome message
        welcomeLabel = new Label("Welcome, " + username + "!");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        welcomeLabel.getStyleClass().add("welcome-label");
        welcomeLabel.setStyle("-fx-text-fill: #e8e6e3;");

        // Game options container
        VBox gameOptionsBox = new VBox(20);
        gameOptionsBox.setAlignment(Pos.CENTER);
        gameOptionsBox.setPadding(new Insets(30, 40, 30, 40));
        gameOptionsBox.getStyleClass().add("form-container"); // Reuse form container styling
        gameOptionsBox.setMaxWidth(500);

        // Play buttons
        Button quickPlayButton = new Button("QUICK PLAY");
        quickPlayButton.setPrefHeight(60);
        quickPlayButton.setMaxWidth(Double.MAX_VALUE);
        quickPlayButton.getStyleClass().add("primary-button");
        quickPlayButton.setOnAction(e -> handleQuickPlay());

        Button playWithFriendButton = new Button("PLAY WITH FRIEND");
        playWithFriendButton.setPrefHeight(60);
        playWithFriendButton.setMaxWidth(Double.MAX_VALUE);
        playWithFriendButton.getStyleClass().add("primary-button");
        playWithFriendButton.setOnAction(e -> handlePlayWithFriend());

        Button createGameButton = new Button("CREATE GAME");
        createGameButton.setPrefHeight(60);
        createGameButton.setMaxWidth(Double.MAX_VALUE);
        createGameButton.getStyleClass().add("secondary-button");
        createGameButton.setOnAction(e -> handleCreateGame());
        createGameButton.setStyle(
                "-fx-background-color: #3d3935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-width: 0;");

        gameOptionsBox.getChildren().addAll(quickPlayButton, playWithFriendButton, createGameButton);

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

        centerContent.getChildren().addAll(welcomeLabel, gameOptionsBox, onlinePlayersBox);

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

        // Friend actions
        VBox friendActionsBox = new VBox(10);
        friendActionsBox.setAlignment(Pos.CENTER);

        Button inviteButton = new Button("Invite to Game");
        inviteButton.setPrefHeight(40);
        inviteButton.setMaxWidth(Double.MAX_VALUE);
        inviteButton.getStyleClass().add("primary-button");
        inviteButton.setOnAction(e -> handleInviteFriend());

        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setPrefHeight(40);
        viewProfileButton.setMaxWidth(Double.MAX_VALUE);
        viewProfileButton.getStyleClass().add("secondary-button");
        viewProfileButton.setOnAction(e -> handleViewProfile());
        viewProfileButton.setStyle(
                "-fx-background-color: #3d3935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-width: 0;");

        friendActionsBox.getChildren().addAll(inviteButton, viewProfileButton);

        friendsPanel.getChildren().addAll(friendsHeader, friendsList, friendActionsBox);

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

    private void handleInviteFriend() {
        String selectedFriend = friendsList.getSelectionModel().getSelectedItem();
        if (selectedFriend != null) {
            System.out.println("Invite friend: " + selectedFriend);
        }
    }

    private void handleViewProfile() {
        String selectedFriend = friendsList.getSelectionModel().getSelectedItem();
        if (selectedFriend != null) {
            System.out.println("View profile: " + selectedFriend);
        }
    }

    public void onAuthSuccess(Message message) {
        this.username = app.getUsername();
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome, " + this.username + "!");
    }

    public void reset() {
        // Reset lobby state if necessary
    }
}
