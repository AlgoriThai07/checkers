import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        try {
            // Store the database file next to the running server jar
            connection = DriverManager.getConnection("jdbc:sqlite:checkers.db");
            connection.setAutoCommit(true);
            initTables();
            System.out.println("[DB] SQLite initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialize SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create the users and game_results tables if they don't already exist
    private void initTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create users table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  username TEXT PRIMARY KEY," +
                "  password_hash TEXT NOT NULL," +
                "  wins INTEGER DEFAULT 0," +
                "  losses INTEGER DEFAULT 0," +
                "  draws INTEGER DEFAULT 0" +
                ")"
            );

            // Create game results table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS game_results (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  winner TEXT," +
                "  loser TEXT," +
                "  is_draw INTEGER DEFAULT 0," +
                "  played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (winner) REFERENCES users(username)," +
                "  FOREIGN KEY (loser)  REFERENCES users(username)" +
                ")"
            );

            // Create friends table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS friends (" +
                "  user1 TEXT NOT NULL," +
                "  user2 TEXT NOT NULL," +
                "  PRIMARY KEY (user1, user2)," +
                "  FOREIGN KEY (user1) REFERENCES users(username)," +
                "  FOREIGN KEY (user2) REFERENCES users(username)" +
                ")"
            );
        }
    }

    // Register user
    public boolean register(String username, String password) {
        // SQL query to insert a new user
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Hash the password
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            ps.setString(1, username);
            ps.setString(2, hash);
            // Execute the query
            ps.executeUpdate();
            System.out.println("[DB] Registered user: " + username);
            return true;
        } catch (SQLException e) {
            // Username already taken
            System.out.println("[DB] Registration failed for " + username + ": " + e.getMessage());
            return false;
        }
    }

    // Login user
    public User login(String username, String password) {
        // SQL query to select user stats
        String sql = "SELECT password_hash, wins, losses, draws FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Set the username parameter
            ps.setString(1, username);
            // Execute the query
            ResultSet rs = ps.executeQuery();
            // Check if the user exists 
            if (rs.next()) {
                // Get the stored password hash
                String storedHash = rs.getString("password_hash");
                // Check if the password is correct
                if (BCrypt.checkpw(password, storedHash)) {
                    // Get the user's stats
                    int wins   = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int draws  = rs.getInt("draws");
                    System.out.println("[DB] Login success: " + username);
                    // Return the user with stats
                    return new User(username, wins, losses, draws);
                } 
                // If password not correct
                else {
                    System.out.println("[DB] Login failed (bad password): " + username);
                    return null;
                }
            } 
            // If user not found
            else {
                System.out.println("[DB] Login failed (user not found): " + username);
                return null;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return null;
        }
    }

    // Record the result of a finished game
    public void recordResult(String winner, String loser, boolean isDraw) {
        try {
            // Update the stats of the winner and loser
            if (isDraw) {
                updateStat(winner, "draws");
                updateStat(loser,  "draws");
            } else {
                updateStat(winner, "wins");
                updateStat(loser,  "losses");
            }

            // Insert a history row
            String sql = "INSERT INTO game_results (winner, loser, is_draw) VALUES (?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, winner);
                ps.setString(2, loser);
                ps.setInt(3, isDraw ? 1 : 0);
                ps.executeUpdate();
            }

            System.out.println("[DB] Recorded result — Winner: " + winner + ", Loser: " + loser + ", Draw: " + isDraw);
        } catch (SQLException e) {
            System.err.println("[DB] Failed to record result: " + e.getMessage());
        }
    }

    // Update a user's stat
    private void updateStat(String username, String column) throws SQLException {
        String sql = "UPDATE users SET " + column + " = " + column + " + 1 WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    // Get a user's current stats
    public User getStats(String username) {
        String sql = "SELECT wins, losses, draws FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            // If user exists, return their stats
            if (rs.next()) {
                return new User(username, rs.getInt("wins"), rs.getInt("losses"), rs.getInt("draws"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] getStats error: " + e.getMessage());
        }
        return null;
    }

    // Check if a user exists in the database
    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            // Return true if user exists
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] userExists error: " + e.getMessage());
            return false;
        }
    }

    // Add a bidirectional friendship between two users
    public boolean addFriend(String user1, String user2) {
        String sql = "INSERT OR IGNORE INTO friends (user1, user2) VALUES (?, ?)";
        try {
            // Insert both directions
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user1);
                ps.setString(2, user2);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user2);
                ps.setString(2, user1);
                ps.executeUpdate();
            }
            System.out.println("[DB] Added friendship: " + user1 + " <-> " + user2);
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] addFriend error: " + e.getMessage());
            return false;
        }
    }

    // Remove a bidirectional friendship between two users
    public void removeFriend(String user1, String user2) {
        String sql = "DELETE FROM friends WHERE user1 = ? AND user2 = ?";
        try {
            // Remove friendship in both directions
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user1);
                ps.setString(2, user2);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user2);
                ps.setString(2, user1);
                ps.executeUpdate();
            }
            System.out.println("[DB] Removed friendship: " + user1 + " <-> " + user2);
        } catch (SQLException e) {
            System.err.println("[DB] removeFriend error: " + e.getMessage());
        }
    }

    // Get all friends of a user
    public String[] getFriends(String username) {
        // Select all friends of a user
        String sql = "SELECT user2 FROM friends WHERE user1 = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            // Create a list to store friends
            List<String> friends = new ArrayList<>();
            // Add each friend to the list
            while (rs.next()) {
                friends.add(rs.getString("user2"));
            }
            // Convert the list to an array and return it
            return friends.toArray(new String[0]);
        } catch (SQLException e) {
            System.err.println("[DB] getFriends error: " + e.getMessage());
            return new String[0];
        }
    }
}
