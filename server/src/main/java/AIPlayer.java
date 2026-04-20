import java.util.ArrayList;
import java.util.List;

import model.GameState;
import model.Move;
import model.PieceType;
import model.Position;

/**
 * AI player using Minimax with Alpha-Beta pruning.
 * Always plays as BLACK.
 */
public class AIPlayer {

    private static final int MAX_DEPTH = 4;

    public Move getMove(GameState state) {
        PieceType[][] board = copyBoard(state.getBoard());
        List<Move> validMoves = state.getValidMoves();

        if (validMoves == null || validMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : validMoves) {
            PieceType[][] newBoard = copyBoard(board);
            applyMove(newBoard, move);

            int score = minimax(newBoard, MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /**
     * Minimax with alpha-beta pruning.
     * @param board current board state
     * @param depth remaining depth to search
     * @param alpha best score the maximizer can guarantee
     * @param beta best score the minimizer can guarantee
     * @param isMaximizing true if it's the AI's turn (BLACK)
     */
    private int minimax(PieceType[][] board, int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0) {
            return evaluate(board);
        }

        String color = isMaximizing ? "BLACK" : "RED";
        List<Move> moves = GameSession.calculateValidMoves(board, color);

        if (moves.isEmpty()) {
            // No moves = this player loses
            return isMaximizing ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                PieceType[][] newBoard = copyBoard(board);
                applyMove(newBoard, move);
                int eval = minimax(newBoard, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break; // prune
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                PieceType[][] newBoard = copyBoard(board);
                applyMove(newBoard, move);
                int eval = minimax(newBoard, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break; // prune
            }
            return minEval;
        }
    }

    /**
     * Evaluation function: (AI pieces) - (opponent pieces).
     * Kings are worth 2, regular pieces are worth 1.
     * AI plays BLACK.
     */
    private int evaluate(PieceType[][] board) {
        int aiScore = 0;
        int opponentScore = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                switch (board[r][c]) {
                    case BLACK:
                        aiScore += 1;
                        break;
                    case BLACK_KING:
                        aiScore += 2;
                        break;
                    case RED:
                        opponentScore += 1;
                        break;
                    case RED_KING:
                        opponentScore += 2;
                        break;
                    default:
                        break;
                }
            }
        }

        return aiScore - opponentScore;
    }

    /**
     * Apply a move to a board copy (does not modify original if board is a copy).
     */
    private void applyMove(PieceType[][] board, Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();

        PieceType piece = board[from.getRow()][from.getCol()];
        board[from.getRow()][from.getCol()] = PieceType.EMPTY;
        board[to.getRow()][to.getCol()] = piece;

        // Remove captured pieces
        for (Position captured : move.getCaptured()) {
            board[captured.getRow()][captured.getCol()] = PieceType.EMPTY;
        }

        // Promote to king
        if (piece == PieceType.RED && to.getRow() == 7) {
            board[to.getRow()][to.getCol()] = PieceType.RED_KING;
        } else if (piece == PieceType.BLACK && to.getRow() == 0) {
            board[to.getRow()][to.getCol()] = PieceType.BLACK_KING;
        }
    }

    private PieceType[][] copyBoard(PieceType[][] board) {
        PieceType[][] copy = new PieceType[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, 8);
        }
        return copy;
    }
}
