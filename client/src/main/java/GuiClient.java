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

    LobbyController lobbyController;
    GameController gameController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        sceneMap = new HashMap<>();

        lobbyController = new LobbyController(this);
        gameController = new GameController(this);

        sceneMap.put("lobby", lobbyController.createScene());
        sceneMap.put("game", gameController.createScene());

        // Create client connection
        clientConnection = new Client(data -> {
            Platform.runLater(() -> handleServerMessage(data));
        });
        clientConnection.start();

        primaryStage.setTitle("Checkers");
        primaryStage.setScene(sceneMap.get("lobby"));
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
                    lobbyController.onAuthSuccess(message);
                    break;
                case AUTH_FAIL:
                    lobbyController.onAuthFail(message);
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
