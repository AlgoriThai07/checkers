package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Move implements Serializable {
    private static final long serialVersionUID = 42L;

    private Position from;
    private Position to;
    private List<Position> captured;

    public Move(Position from, Position to, List<Position> captured) {
        this.from = from;
        this.to = to;
        this.captured = (captured != null) ? captured : new ArrayList<>();
    }

    public Move(Position from, Position to) {
        this(from, to, new ArrayList<>());
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public List<Position> getCaptured() {
        return captured;
    }

    @Override
    public String toString() {
        return from + " -> " + to + (captured.isEmpty() ? "" : " captured: " + captured);
    }
}
