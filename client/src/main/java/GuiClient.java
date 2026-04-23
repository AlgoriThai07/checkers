import java.io.Serializable;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import model.Message;

public class GuiClient extends Application {

    // Keep 1 scene and only swap the root
    private final HashMap<String, Parent> rootMap = new HashMap<>();
    private final HashMap<String, double[]> sizeMap = new HashMap<>();
    private Scene sharedScene;
    Client clientConnection;
    Stage primaryStage;

    String username = "";

    // Use 1 controller for each screen
    LoginController loginController;
    SignUpController signUpController; 
    LobbyController lobbyController;
    GameController gameController;
    MatchmakingController matchmakingController;
    
    // Track the current scene
    private String currentSceneName = "login";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // Build all controllers
        loginController = new LoginController(this);
        signUpController = new SignUpController(this);
        lobbyController = new LobbyController(this);
        gameController = new GameController(this);
        matchmakingController = new MatchmakingController(this);

        // Each controller builds its own scene with a preferred size 
        registerScreen("login", loginController.createScene());
        registerScreen("signup", signUpController.createScene());
        registerScreen("lobby", lobbyController.createScene());
        registerScreen("game", gameController.createScene());
        registerScreen("matchmaking", matchmakingController.createScene());

        // Create 1 scene for the initial screen to be reused
        double[] initialSize = sizeMap.get("login");
        sharedScene = new Scene(rootMap.get("login"), initialSize[0], initialSize[1]);
        try {
            // Shared stylesheet
            sharedScene.getStylesheets().add(getClass().getResource("/app-styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load /app-styles.css");
        }

        // Connect to server
        clientConnection = new Client(data -> {
            Platform.runLater(() -> handleServerMessage(data));
        });
        clientConnection.start();

        primaryStage.setTitle("Checkers");
        primaryStage.setScene(sharedScene);
        // Exit the application when the window is closed
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    // Store the root and size of each screen
    private void registerScreen(String name, Scene original) {
        sizeMap.put(name, new double[] { original.getWidth(), original.getHeight() });
        Parent root = original.getRoot();
        original.setRoot(new Group());
        rootMap.put(name, root);
    }

    // Handle server messages to call the suitable controller
    private void handleServerMessage(Serializable data) {
        if (data instanceof Message) {
            Message message = (Message) data;
            System.out.println("Received: " + message.getType());

            switch (message.getType()) {
                // Authentication successful
                case AUTH_SUCCESS:
                    this.username = message.getSender() != null ? message.getSender() : username;
                    lobbyController.onAuthSuccess(message); // logging in successful
                    matchmakingController.setUsername(this.username);
                    switchToScene("lobby"); 
                    break;
                // Authentication failed
                case AUTH_FAIL:
                    if ("login".equals(currentSceneName)) {
                        loginController.showError(message.getContent() != null ? message.getContent() : "Auth Failed"); 
                    } else if ("signup".equals(currentSceneName)) {
                        signUpController.showError(message.getContent() != null ? message.getContent() : "Auth Failed"); 
                    }
                    break;
                // Game started
                case GAME_START:
                    gameController.onGameStart(message);
                    switchToScene("game");
                    break;
                case GAME_UPDATE:
                    gameController.onGameUpdate(message);
                    break;
                case INVALID_MOVE:
                    gameController.onInvalidMove(message);
                    break;
                case GAME_OVER:
                    gameController.onGameOver(message);
                    break;
                case CHAT:
                    gameController.onChat(message);
                    break;
                case QUIT:
                    gameController.onOpponentQuit(message);
                    break;
                case DRAW_OFFER:
                    gameController.onDrawOffer(message);
                    break;
                case DRAW_DECLINE:
                    gameController.onDrawDecline(message);
                    break;
                case STATS_UPDATE:
                    lobbyController.onStatsUpdate(message);
                    break;
                case FRIENDS_LIST_UPDATE:
                    lobbyController.onFriendsListUpdate(message);
                    break;
                case ADD_FRIEND:
                    lobbyController.onAddFriendResponse(message);
                    break;
                case MATCH_INVITE:
                    lobbyController.onMatchInvite(message);
                    break;
                case MATCH_INVITE_CANCEL:
                    lobbyController.onMatchInviteCancel(message);
                    break;
                case MATCH_INVITE_RESPONSE:
                    lobbyController.onMatchInviteResponse(message);
                    break;
                default:
                    break;
            }
        } else {
            // Non-message received
            System.out.println("Non-message received: " + data);
        }
    }

    // Switch to a new scene
    public void switchToScene(String sceneName) {
        currentSceneName = sceneName;

        // Timer management for matchmaking
        if (sceneName.equals("matchmaking")) {
            matchmakingController.startTimer();
        } else {
            matchmakingController.stopTimer();
        }

        // Reset screens when switching to a new scene
        if (sceneName.equals("lobby")) {
            lobbyController.reset();
        } else if (sceneName.equals("login")) {
            loginController.reset();
        }

        // Swap the root instead of whole scene
        Parent newRoot = rootMap.get(sceneName);
        if (newRoot == null) return;
        sharedScene.setRoot(newRoot);

        // Only auto-resize when the screen is not maximized 
        if (!primaryStage.isMaximized() && !primaryStage.isFullScreen()) {
            double[] size = sizeMap.get(sceneName);
            if (size != null && size[0] > 0 && size[1] > 0) {
                primaryStage.setWidth(size[0]);
                primaryStage.setHeight(size[1]);
            }
        }
    }

    public void send(Message message) {
        clientConnection.send(message);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
