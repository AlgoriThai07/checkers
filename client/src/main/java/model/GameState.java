package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private PieceType[][] board;
    private String currentTurn;
    private String redPlayer;
    private String blackPlayer;
    private String status;
    private List<Move> validMoves;

    public GameState() {
        this.board = new PieceType[8][8];
        this.currentTurn = "RED";
        this.status = "IN_PROGRESS";
        this.validMoves = new ArrayList<>();
    }

    public PieceType[][] getBoard() {
        return board;
    }

    public void setBoard(PieceType[][] board) {
        this.board = board;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getRedPlayer() {
        return redPlayer;
    }

    public void setRedPlayer(String redPlayer) {
        this.redPlayer = redPlayer;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(String blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Move> getValidMoves() {
        return validMoves;
    }

    public void setValidMoves(List<Move> validMoves) {
        this.validMoves = validMoves;
    }
}
