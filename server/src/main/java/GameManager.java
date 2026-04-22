import java.util.ArrayList;
import java.util.List;

import model.Message;
import model.Message.MessageType;

public class GameManager {

    private List<ClientHandler> waitingQueue;
    private List<GameSession> activeSessions;
    private List<ClientHandler> activeClients;
    private DatabaseManager databaseManager;

    public GameManager(DatabaseManager databaseManager) {
        this.waitingQueue = new ArrayList<>();
        this.activeSessions = new ArrayList<>();
        this.activeClients = new ArrayList<>();
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

    public synchronized void addClient(ClientHandler client) {
        if (!activeClients.contains(client)) {
            activeClients.add(client);
        }
        // Send this client their friends list with online status
        sendFriendsListTo(client);
        // Notify any online friends that this user came online
        notifyFriendsOfStatusChange(client);
    }

    public synchronized void removeClient(ClientHandler client) {
        activeClients.remove(client);
        waitingQueue.remove(client);
        // Notify any online friends that this user went offline
        notifyFriendsOfStatusChange(client);
    }

    /**
     * Send a FRIENDS_LIST_UPDATE to the given client.
     * Format: "friend1:online,friend2:offline,friend3:online"
     */
    public synchronized void sendFriendsListTo(ClientHandler client) {
        String[] friends = databaseManager.getFriends(client.getUsername());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < friends.length; i++) {
            if (i > 0) sb.append(",");
            boolean isOnline = isUserOnline(friends[i]);
            sb.append(friends[i]).append(":").append(isOnline ? "online" : "offline");
        }
        Message msg = new Message(MessageType.FRIENDS_LIST_UPDATE, sb.toString());
        client.sendMessage(msg);
    }

    /**
     * When a user comes online or goes offline, notify each of their friends
     * who is currently online so they can refresh their friends list.
     */
    private void notifyFriendsOfStatusChange(ClientHandler client) {
        String[] friends = databaseManager.getFriends(client.getUsername());
        for (String friendName : friends) {
            ClientHandler friendClient = getClientByUsername(friendName);
            if (friendClient != null) {
                sendFriendsListTo(friendClient);
            }
        }
    }

    /**
     * Handle an ADD_FRIEND request from a client.
     */
    public synchronized void handleAddFriend(ClientHandler client, String friendUsername) {
        if (friendUsername == null || friendUsername.trim().isEmpty()) {
            client.sendMessage(new Message(MessageType.ADD_FRIEND, "ERROR:Username cannot be empty"));
            return;
        }
        if (friendUsername.equals(client.getUsername())) {
            client.sendMessage(new Message(MessageType.ADD_FRIEND, "ERROR:You cannot add yourself"));
            return;
        }
        if (!databaseManager.userExists(friendUsername)) {
            client.sendMessage(new Message(MessageType.ADD_FRIEND, "ERROR:User not found"));
            return;
        }
        boolean success = databaseManager.addFriend(client.getUsername(), friendUsername);
        if (success) {
            client.sendMessage(new Message(MessageType.ADD_FRIEND, "SUCCESS:" + friendUsername));
            // Refresh both users' friends lists
            sendFriendsListTo(client);
            ClientHandler friendClient = getClientByUsername(friendUsername);
            if (friendClient != null) {
                sendFriendsListTo(friendClient);
            }
        } else {
            client.sendMessage(new Message(MessageType.ADD_FRIEND, "ERROR:Could not add friend"));
        }
    }

    /**
     * Handle a REMOVE_FRIEND request from a client.
     */
    public synchronized void handleRemoveFriend(ClientHandler client, String friendUsername) {
        databaseManager.removeFriend(client.getUsername(), friendUsername);
        client.sendMessage(new Message(MessageType.REMOVE_FRIEND, "SUCCESS:" + friendUsername));
        // Refresh both users' friends lists
        sendFriendsListTo(client);
        ClientHandler friendClient = getClientByUsername(friendUsername);
        if (friendClient != null) {
            sendFriendsListTo(friendClient);
        }
    }

    /**
     * Check if a user is currently online (has an active client connection).
     */
    private boolean isUserOnline(String username) {
        for (ClientHandler client : activeClients) {
            if (username.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a user is currently in an active game session.
     */
    private boolean isInGame(String username) {
        for (GameSession session : activeSessions) {
            if (session.hasPlayer(username)) return true;
        }
        return false;
    }

    /**
     * Handle when a user sends a match invite to a friend.
     */
    public synchronized void handleMatchInvite(ClientHandler sender, String targetUsername) {
        ClientHandler targetClient = getClientByUsername(targetUsername);
        if (targetClient == null) {
            sender.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "offline"));
            return;
        }

        if (isInGame(targetUsername)) {
            sender.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "in_game"));
            return;
        }

        // Remove sender from regular matchmaking queue just in case
        waitingQueue.remove(sender);

        // Forward invite to receiver
        targetClient.sendMessage(new Message(MessageType.MATCH_INVITE, sender.getUsername()));
    }

    /**
     * Handle when sender cancels the match invite.
     */
    public synchronized void handleMatchInviteCancel(ClientHandler sender, String targetUsername) {
        ClientHandler targetClient = getClientByUsername(targetUsername);
        if (targetClient != null) {
            targetClient.sendMessage(new Message(MessageType.MATCH_INVITE_CANCEL, sender.getUsername()));
        }
    }

    /**
     * Handle when receiver accepts the match invite.
     */
    public synchronized void handleMatchInviteAccept(ClientHandler receiver, String senderUsername) {
        ClientHandler senderClient = getClientByUsername(senderUsername);
        if (senderClient == null) {
            // Sender went offline
            receiver.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "offline"));
            return;
        }
        
        if (isInGame(senderUsername)) {
            // Sender started another game
            receiver.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "in_game"));
            return;
        }

        // Remove both from waiting queue if they are in it
        waitingQueue.remove(senderClient);
        waitingQueue.remove(receiver);

        // Notify sender that invite was accepted so they can close their modal
        senderClient.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "accepted"));

        // Create game session
        GameSession session = new GameSession(senderClient, receiver, null, this, databaseManager);
        activeSessions.add(session);
        session.startGame();
        System.out.println("Friend PVP game started: " + senderUsername + " vs " + receiver.getUsername());
    }

    /**
     * Handle when receiver declines the match invite.
     */
    public synchronized void handleMatchInviteDecline(ClientHandler receiver, String senderUsername) {
        ClientHandler senderClient = getClientByUsername(senderUsername);
        if (senderClient != null) {
            senderClient.sendMessage(new Message(MessageType.MATCH_INVITE_RESPONSE, "declined"));
        }
    }

    /**
     * Find an active ClientHandler by username.
     */
    private ClientHandler getClientByUsername(String username) {
        for (ClientHandler client : activeClients) {
            if (username.equals(client.getUsername())) {
                return client;
            }
        }
        return null;
    }
}

