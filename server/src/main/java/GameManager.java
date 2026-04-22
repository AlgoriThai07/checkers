import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private List<ClientHandler> waitingQueue;
    private List<GameSession> activeSessions;
    private DatabaseManager databaseManager;

    public GameManager(DatabaseManager databaseManager) {
        this.waitingQueue = new ArrayList<>();
        this.activeSessions = new ArrayList<>();
        this.databaseManager = databaseManager;
    }

    public synchronized void addToQueue(ClientHandler client, String mode) {
        // If user choose AI mode
        if ("AI".equalsIgnoreCase(mode)) {
            // Create an AI game immediately
            AIPlayer aiPlayer = new AIPlayer();
            // Create a new game session
            GameSession session = new GameSession(client, null, aiPlayer, this, databaseManager);
            activeSessions.add(session);
            // Start the game
            session.startGame();
            System.out.println("AI game started for " + client.getUsername());
        }
        // If user choose local hot-seat mode
        else if ("LOCAL".equalsIgnoreCase(mode)) {
            // Create a local game with no second client and no AI
            GameSession session = new GameSession(client, null, null, this, databaseManager);
            activeSessions.add(session);
            session.startGame();
            System.out.println("Local hot-seat game started for " + client.getUsername());
        }
        // If user choose vs human mode (online PVP)
        else {
            // Add to queue
            waitingQueue.add(client);
            System.out.println(client.getUsername() + " added to PVP queue. Queue size: " + waitingQueue.size());
            // If the queue is larger than 2, match the first 2 player
            if (waitingQueue.size() >= 2) {
                ClientHandler player1 = waitingQueue.remove(0);
                ClientHandler player2 = waitingQueue.remove(0);
                // Create a new game session
                GameSession session = new GameSession(player1, player2, null, this, databaseManager);
                activeSessions.add(session);
                // Start the game
                session.startGame();
                System.out.println("PVP game started: " + player1.getUsername() + " vs " + player2.getUsername());
            }
        }
    }

    public synchronized void removeSession(GameSession session) {
        activeSessions.remove(session);
    }

    public synchronized void removeFromQueue(ClientHandler client) {
        waitingQueue.remove(client);
    }
}
