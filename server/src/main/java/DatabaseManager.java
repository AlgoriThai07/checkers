import model.User;

public class DatabaseManager {

    public DatabaseManager() {
        // Partner B will add SQLite initialization here
    }

    /**
     * Register a new user. Partner B will implement with SQLite + BCrypt.
     * Stub: always returns true.
     */
    public boolean register(String username, String password) {
        System.out.println("[DB STUB] Register: " + username);
        return true;
    }

    /**
     * Login an existing user. Partner B will implement with SQLite + BCrypt.
     * Stub: always returns a new User.
     */
    public User login(String username, String password) {
        System.out.println("[DB STUB] Login: " + username);
        return new User(username);
    }

    /**
     * Record the result of a game. Partner B will implement with SQLite.
     * Stub: no-op.
     */
    public void recordResult(String winner, String loser, boolean isDraw) {
        System.out.println("[DB STUB] Result - Winner: " + winner + ", Loser: " + loser + ", Draw: " + isDraw);
    }
}
