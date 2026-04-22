package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;
import model.User;

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

    /**
     * Create the users and game_results tables if they don't already exist.
     */
    private void initTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  username TEXT PRIMARY KEY," +
                "  password_hash TEXT NOT NULL," +
                "  wins INTEGER DEFAULT 0," +
                "  losses INTEGER DEFAULT 0," +
                "  draws INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
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
        }
    }

    /**
     * Register a new user with a BCrypt-hashed password.
     * @return true if registration succeeded, false if the username already exists.
     */
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();
            System.out.println("[DB] Registered user: " + username);
            return true;
        } catch (SQLException e) {
            // UNIQUE constraint violation means username already taken
            System.out.println("[DB] Registration failed for " + username + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Authenticate a user by verifying their password against the stored BCrypt hash.
     * @return a User object with stats if credentials are valid, null otherwise.
     */
    public User login(String username, String password) {
        String sql = "SELECT password_hash, wins, losses, draws FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    int wins   = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int draws  = rs.getInt("draws");
                    System.out.println("[DB] Login success: " + username);
                    return new User(username, wins, losses, draws);
                } else {
                    System.out.println("[DB] Login failed (bad password): " + username);
                    return null;
                }
            } else {
                System.out.println("[DB] Login failed (user not found): " + username);
                return null;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Record the result of a finished game.
     * Updates win/loss/draw counters for both players and inserts a game_results row.
     */
    public void recordResult(String winner, String loser, boolean isDraw) {
        try {
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

            System.out.println("[DB] Recorded result - Winner: " + winner + ", Loser: " + loser + ", Draw: " + isDraw);
        } catch (SQLException e) {
            System.err.println("[DB] Failed to record result: " + e.getMessage());
        }
    }

    /**
     * Increment a specific stat column for a user.
     */
    private void updateStat(String username, String column) throws SQLException {
        // column is always one of "wins", "losses", "draws" - safe to inline
        String sql = "UPDATE users SET " + column + " = " + column + " + 1 WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieve a user's current stats.
     * @return User with stats, or null if not found.
     */
    public User getStats(String username) {
        String sql = "SELECT wins, losses, draws FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(username, rs.getInt("wins"), rs.getInt("losses"), rs.getInt("draws"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] getStats error: " + e.getMessage());
        }
        return null;
    }
}
