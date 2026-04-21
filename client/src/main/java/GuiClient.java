import java.io.Serializable;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import model.Message;
import model.Message.MessageType;

public class GuiClient extends Application {

    HashMap<String, Scene> sceneMap;
    Client clientConnection;
    Stage primaryStage;

    String username = "";

    LoginController loginController; // ADDED: Controller for our awesome new Figma Login Screen
    SignUpController signUpController; // ADDED: Controller for our Sign Up Screen
    LobbyController lobbyController;
    GameController gameController;
    MatchmakingController matchmakingController;
    
    private String currentSceneName = "login";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        sceneMap = new HashMap<>();

        loginController = new LoginController(this); // ADDED: Instantiate the login controller
        signUpController = new SignUpController(this); // ADDED: Instantiate the sign up controller
        lobbyController = new LobbyController(this);
        gameController = new GameController(this);
        matchmakingController = new MatchmakingController(this);

        sceneMap.put("login", loginController.createScene()); // ADDED: Register the login scene
        sceneMap.put("signup", signUpController.createScene()); // ADDED: Register the sign up scene
        sceneMap.put("lobby", lobbyController.createScene());
        sceneMap.put("game", gameController.createScene());
        sceneMap.put("matchmaking", matchmakingController.createScene());

        // Create client connection
        clientConnection = new Client(data -> {
            Platform.runLater(() -> handleServerMessage(data));
        });
        clientConnection.start();

        primaryStage.setTitle("Checkers");
        primaryStage.setScene(sceneMap.get("login")); // ADDED/CHANGED: START AT LOGIN INSTEAD OF LOBBY
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    private void handleServerMessage(Serializable data) {
        if (data instanceof Message) {
            Message message = (Message) data;
            System.out.println("Received: " + message.getType());

            switch (message.getType()) {
                case AUTH_SUCCESS:
                    this.username = message.getSender() != null ? message.getSender() : username;
                    lobbyController.onAuthSuccess(message); // tell lobby we logged in
                    matchmakingController.setUsername(this.username);
                    switchToScene("lobby"); // ADDED: After login success, automatically SWITCH TO LOBBY
                    break;
                case AUTH_FAIL:
                    if ("login".equals(currentSceneName)) {
                        loginController.showError(message.getContent() != null ? message.getContent() : "Auth Failed"); 
                    } else if ("signup".equals(currentSceneName)) {
                        signUpController.showError(message.getContent() != null ? message.getContent() : "Auth Failed"); 
                    }
                    break;
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
                    switchToScene("lobby");
                    break;
                default:
                    break;
            }
        } else {
            System.out.println("Non-message received: " + data);
        }
    }

    public void switchToScene(String sceneName) {
        currentSceneName = sceneName;
        
        // Timer management
        if (sceneName.equals("matchmaking")) {
            matchmakingController.startTimer();
        } else {
            matchmakingController.stopTimer();
        }

        if (sceneName.equals("lobby")) {
            lobbyController.reset();
        }
        primaryStage.setScene(sceneMap.get(sceneName));
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
