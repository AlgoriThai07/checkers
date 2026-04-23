import java.util.List;

import model.GameState;
import model.Move;
import model.PieceType;
import model.Position;

// Implement AI player using minimax algorithm with alpha-beta pruning
// AI player always play as Black
public class AIPlayer {
    // Keep the difficulty intermediate
    private static final int MAX_DEPTH = 6;

    public Move getMove(GameState state) {
        // Get current board state
        PieceType[][] board = copyBoard(state.getBoard());
        // Get valid moves
        List<Move> validMoves = state.getValidMoves();
        // If there no valid moves, return null
        if (validMoves == null || validMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        // For every valid move
        for (Move move : validMoves) {
            // Apply that move
            PieceType[][] newBoard = copyBoard(board);
            applyMove(newBoard, move);
            // Calculate the score (how good or bad) that move is for the AI
            int score = minimax(newBoard, MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            // Update the best score and best move if score is higher
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        // Return best move
        return bestMove;
    }

    // Minimax algorithm
    // board: current board state
    // depth: remaining depth to search
    // alpha: best score maximizer can guarantee
    // beta: best score minimizer can guarantee
    // isMaximizing: true if AI's turn
    private int minimax(PieceType[][] board, int depth, int alpha, int beta, boolean isMaximizing) {
        // If no remaining depth, evaluate the board
        if (depth == 0) {
            return evaluate(board);
        }

        // If true, black turn
        // Else, red turn
        String color = isMaximizing ? "BLACK" : "RED";
        // Return a list of valid moves
        List<Move> moves = GameSession.calculateValidMoves(board, color);

        // If no valid moves, this player lose
        if (moves.isEmpty()) {
            // If AI loses, return MIN_VALUE + 1 = Guaranteed loss for AI
            // If human loses, return MAX_VALUE - 1 = Guaranteed win for AI
            return isMaximizing ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;
        }

        // If Ai turn
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                // Go through every move
                PieceType[][] newBoard = copyBoard(board);
                applyMove(newBoard, move);
                int eval = minimax(newBoard, depth - 1, alpha, beta, false);
                // Update the maxEval and alpha if needed
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                // Prune this branch
                // The move is so good that human won't allow it happen
                if (beta <= alpha) break;
            }
            return maxEval;
        }
        // Else human turn
        else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                // Go through every move
                PieceType[][] newBoard = copyBoard(board);
                applyMove(newBoard, move);
                int eval = minimax(newBoard, depth - 1, alpha, beta, true);
                // Update minEval and beta if needed
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                // Prune this branch
                // The move is so bad for AI that it won't allow this happen
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    // Evaluate the current board state
    private int evaluate(PieceType[][] board) {
        int aiScore = 0;
        int opponentScore = 0;
        // Regular pieces worth 1
        // Kings worth 2
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
        // If score > 0, AI is winning
        // else if score < 0, human is winning,
        // Else, draw
        return aiScore - opponentScore;
    }

    // Apply a move
    private void applyMove(PieceType[][] board, Move move) {
        // Get the from and to
        Position from = move.getFrom();
        Position to = move.getTo();

        // Get the piece and move it
        // Set FROM to be empty
        // Set TO to be that piece
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
    // Create a copy of current board
    private PieceType[][] copyBoard(PieceType[][] board) {
        PieceType[][] copy = new PieceType[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, 8);
        }
        return copy;
    }
}
