import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mindrot.jbcrypt.BCrypt;

import model.User;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:checkers.db";

    public DatabaseManager() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // Create the users table if it doesn't already exist
            String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                    + " username TEXT UNIQUE NOT NULL,\n"
                    + " password_hash TEXT NOT NULL,\n"
                    + " wins INTEGER DEFAULT 0,\n"
                    + " losses INTEGER DEFAULT 0,\n"
                    + " draws INTEGER DEFAULT 0\n"
                    + ");";
            stmt.execute(sql);
            System.out.println("Database initialized successfully.");
            
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Register a new user.
     * Uses BCrypt to hash the password before saving to SQLite.
     * Returns true if successful, false if username is taken or error occurs.
     */
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            // Hash the password securely
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
            
            System.out.println("Successfully registered new user: " + username);
            return true;
            
        } catch (SQLException e) {
            System.out.println("Registration error (username may exist): " + e.getMessage());
            return false;
        }
    }

    /**
     * Login an existing user.
     * Looks up user by username and checks the password hash via BCrypt.
     * Returns heavily populated User object on success, or null on failure.
     */
    public User login(String username, String password) {
        String sql = "SELECT username, password_hash, wins, losses, draws FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                // Verify provided password matches the securely stored hash
                if (BCrypt.checkpw(password, storedHash)) {
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int draws = rs.getInt("draws");
                    
                    System.out.println("Successful login for user: " + username);
                    return new User(username, wins, losses, draws);
                } else {
                    System.out.println("Failed login attempt (wrong password) for: " + username);
                }
            } else {
                System.out.println("Failed login attempt (unknown user): " + username);
            }
            
        } catch (SQLException e) {
            System.out.println("Login database error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Record the result of a game. Partner B will implement with SQLite.
     */
    public void recordResult(String winner, String loser, boolean isDraw) {
        // We will implement the game counting updates later today!
        System.out.println("[DB STUB] Result - Winner: " + winner + ", Loser: " + loser + ", Draw: " + isDraw);
    }
}
