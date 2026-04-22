import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import model.Message;
import model.Message.MessageType;
import model.User;

public class ClientHandler extends Thread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private GameManager gameManager;
    private DatabaseManager databaseManager;
    private String username;
    private GameSession currentSession;

    public ClientHandler(Socket socket, GameManager gameManager, DatabaseManager databaseManager) {
        this.socket = socket;
        this.gameManager = gameManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            socket.setTcpNoDelay(true);
        } catch (Exception e) {
            System.out.println("Error setting up streams: " + e.getMessage());
            return;
        }

        while (true) {
            try {
                Message message = (Message) in.readObject();
                System.out.println("Received " + message.getType() + " from " + (username != null ? username : "unknown"));

                switch (message.getType()) {
                    case REGISTER:
                        handleRegister(message);
                        break;
                    case LOGIN:
                        handleLogin(message);
                        break;
                    case QUEUE:
                        handleQueue(message);
                        break;
                    case MOVE:
                        handleMove(message);
                        break;
                    case CHAT:
                        handleChat(message);
                        break;
                    case PLAY_AGAIN:
                        handlePlayAgain();
                        break;
                    case QUIT:
                        handleQuit();
                        break;
                    case DRAW_OFFER:
                        if (currentSession != null) currentSession.forwardDrawOffer(this);
                        break;
                    case DRAW_ACCEPT:
                        if (currentSession != null) currentSession.handleDrawAccept(this);
                        break;
                    case DRAW_DECLINE:
                        if (currentSession != null) currentSession.handleDrawDecline(this);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
                handleDisconnect();
                break;
            }
        }
    }

    private void handleRegister(Message message) {
        String[] parts = message.getContent().split(":");
        if (parts.length == 2) {
            boolean success = databaseManager.register(parts[0], parts[1]);
            if (success) {
                this.username = parts[0];
                Message response = new Message(MessageType.AUTH_SUCCESS, "Registration successful");
                response.setSender(username);
                sendMessage(response);
                gameManager.addClient(this);
            } else {
                sendMessage(new Message(MessageType.AUTH_FAIL, "Username already taken"));
            }
        } else {
            sendMessage(new Message(MessageType.AUTH_FAIL, "Invalid registration format"));
        }
    }

    private void handleLogin(Message message) {
        String[] parts = message.getContent().split(":");
        if (parts.length == 2) {
            User user = databaseManager.login(parts[0], parts[1]);
            if (user != null) {
                this.username = parts[0];
                String statsPayload = "Login successful:" + user.getWins() + ":" + user.getLosses() + ":" + user.getDraws();
                Message response = new Message(MessageType.AUTH_SUCCESS, statsPayload);
                response.setSender(username);
                sendMessage(response);
                gameManager.addClient(this);
            } else {
                sendMessage(new Message(MessageType.AUTH_FAIL, "Invalid credentials"));
            }
        } else {
            sendMessage(new Message(MessageType.AUTH_FAIL, "Invalid login format"));
        }
    }

    private void handleQueue(Message message) {
        gameManager.addToQueue(this, message.getContent());
    }

    private void handleMove(Message message) {
        if (currentSession != null) {
            currentSession.handleMove(this, message.getMove());
        }
    }

    private void handleChat(Message message) {
        if (currentSession != null) {
            message.setSender(username);
            currentSession.forwardChat(this, message);
        }
    }

    private void handlePlayAgain() {
        if (currentSession != null) {
            currentSession.handlePlayAgain(this);
        }
    }

    private void handleQuit() {
        if (currentSession != null) {
            currentSession.handleQuit(this);
        }
    }

    private void handleDisconnect() {
        gameManager.removeClient(this);
        if (currentSession != null) {
            currentSession.handleQuit(this);
        }
        try {
            socket.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (Exception e) {
            System.out.println("Error sending message to " + (username != null ? username : "unknown"));
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public GameSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(GameSession session) {
        this.currentSession = session;
    }
}
