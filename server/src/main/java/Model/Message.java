package model;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    public enum MessageType {
        REGISTER,
        LOGIN,
        AUTH_SUCCESS,
        AUTH_FAIL,
        QUEUE,
        GAME_START,
        MOVE,
        GAME_UPDATE,
        INVALID_MOVE,
        GAME_OVER,
        CHAT,
        PLAY_AGAIN,
        QUIT,
        DRAW_OFFER,
        DRAW_ACCEPT,
        DRAW_DECLINE,
        STATS_UPDATE,
        ADD_FRIEND,
        REMOVE_FRIEND,
        FRIENDS_LIST_UPDATE,
        MATCH_INVITE,
        MATCH_INVITE_CANCEL,
        MATCH_INVITE_ACCEPT,
        MATCH_INVITE_DECLINE,
        MATCH_INVITE_RESPONSE,
        UNDO
    }

    private MessageType type;
    private String sender;
    private GameState gameState;
    private Move move;
    private String content;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public Message(MessageType type, GameState gameState) {
        this.type = type;
        this.gameState = gameState;
    }

    public Message(MessageType type, Move move) {
        this.type = type;
        this.move = move;
    }

    public Message(MessageType type, String sender, GameState gameState, Move move, String content) {
        this.type = type;
        this.sender = sender;
        this.gameState = gameState;
        this.move = move;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public Move getMove() {
        return move;
    }

    public void setMove(Move move) {
        this.move = move;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message[type=" + type + ", sender=" + sender + ", content=" + content + "]";
    }
}
