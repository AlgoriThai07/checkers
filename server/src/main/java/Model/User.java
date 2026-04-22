package model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 42L;

    private String username;
    private int wins;
    private int losses;
    private int draws;

    public User(String username) {
        this.username = username;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
    }

    public User(String username, int wins, int losses, int draws) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }

    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }
}
