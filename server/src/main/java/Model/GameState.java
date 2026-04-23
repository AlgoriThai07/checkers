package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 42L;

    private PieceType[][] board;
    private String currentTurn;
    private String redPlayer;
    private String blackPlayer;
    private String status;
    private List<Move> validMoves;
    private int historyCount;

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

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int historyCount) {
        this.historyCount = historyCount;
    }

    public GameState deepCopy() {
        GameState copy = new GameState();
        copy.setCurrentTurn(this.currentTurn);
        copy.setRedPlayer(this.redPlayer);
        copy.setBlackPlayer(this.blackPlayer);
        copy.setStatus(this.status);
        copy.setHistoryCount(this.historyCount);
        
        PieceType[][] newBoard = new PieceType[8][8];
        for(int r = 0; r < 8; r++){
            for(int c = 0; c < 8; c++){
                newBoard[r][c] = this.board[r][c];
            }
        }
        copy.setBoard(newBoard);
        
        List<Move> newMoves = new ArrayList<>();
        if (this.validMoves != null) {
            for(Move m : this.validMoves) {
                List<Position> newCaptured = new ArrayList<>();
                for(Position p : m.getCaptured()){
                    newCaptured.add(new Position(p.getRow(), p.getCol()));
                }
                newMoves.add(new Move(
                    new Position(m.getFrom().getRow(), m.getFrom().getCol()),
                    new Position(m.getTo().getRow(), m.getTo().getCol()),
                    newCaptured
                ));
            }
        }
        copy.setValidMoves(newMoves);
        return copy;
    }
}
