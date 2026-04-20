import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;
    private GameManager gameManager;
    private DatabaseManager databaseManager;

    public Server(int port) {
        this.port = port;
        this.databaseManager = new DatabaseManager();
        this.gameManager = new GameManager(databaseManager);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Checkers Server started on port " + port);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, gameManager, databaseManager);
                handler.start();
            }
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
