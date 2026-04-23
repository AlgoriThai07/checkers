import java.util.ArrayList;
import java.util.List;

import model.User;

import model.GameState;
import model.Message;
import model.Message.MessageType;
import model.Move;
import model.PieceType;
import model.Position;

public class GameSession {

    private ClientHandler redPlayer;
    private ClientHandler blackPlayer;
    private AIPlayer aiPlayer;
    private GameState gameState;
    private GameManager gameManager;
    private DatabaseManager databaseManager;
    private boolean isAIGame;
    private boolean isLocalGame;
    private boolean redWantsPlayAgain = false;
    private boolean blackWantsPlayAgain = false;

    public GameSession(ClientHandler redPlayer, ClientHandler blackPlayer, AIPlayer aiPlayer,
            GameManager gameManager, DatabaseManager databaseManager) {
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.aiPlayer = aiPlayer;
        this.gameManager = gameManager;
        this.databaseManager = databaseManager;
        this.isAIGame = (aiPlayer != null);
        this.isLocalGame = (blackPlayer == null && aiPlayer == null);
        this.gameState = new GameState();

        // Link handlers to this session
        redPlayer.setCurrentSession(this);
        // If a PvP game
        if (blackPlayer != null) {
            blackPlayer.setCurrentSession(this);
        }
    }

    public boolean hasPlayer(String username) {
        if (redPlayer != null && username.equals(redPlayer.getUsername()))
            return true;
        if (blackPlayer != null && username.equals(blackPlayer.getUsername()))
            return true;
        return false;
    }

    public void startGame() {
        // Init game
        initializeBoard();
        gameState.setRedPlayer(redPlayer.getUsername());
        if (isAIGame) {
            gameState.setBlackPlayer("AI");
        } else if (isLocalGame) {
            gameState.setBlackPlayer("Opponent");
        } else {
            gameState.setBlackPlayer(blackPlayer.getUsername());
        }
        gameState.setCurrentTurn("RED");
        gameState.setStatus("IN_PROGRESS");

        // Calculate valid moves for RED
        gameState.setValidMoves(calculateValidMoves(gameState.getBoard(), "RED"));

        // Send GAME_START to both players
        Message startMessage = new Message(MessageType.GAME_START, gameState);
        redPlayer.sendMessage(startMessage);
        if (blackPlayer != null) {
            blackPlayer.sendMessage(startMessage);
        }

        redWantsPlayAgain = false;
        blackWantsPlayAgain = false;
    }

    private void initializeBoard() {
        PieceType[][] board = new PieceType[8][8];
        // Set all squares to empty first
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = PieceType.EMPTY;
            }
        }

        // Red pieces on rows 0-2, dark squares only ((row + col) is odd)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    board[r][c] = PieceType.RED;
                }
            }
        }

        // Black pieces on rows 5-7, dark squares only
        for (int r = 5; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    board[r][c] = PieceType.BLACK;
                }
            }
        }

        gameState.setBoard(board);
    }

    public synchronized void handleMove(ClientHandler sender, Move move) {
        // In local mode, the single client controls both sides
        String senderColor;
        if (isLocalGame) {
            senderColor = gameState.getCurrentTurn();
        } else {
            // Get color of player wanting to move
            senderColor = getPlayerColor(sender);
            // If not his turn, not allow to move
            if (senderColor == null || !senderColor.equals(gameState.getCurrentTurn())) {
                sender.sendMessage(new Message(MessageType.INVALID_MOVE, "Not your turn"));
                return;
            }
        }

        // Find matching valid move (has captured list populated)
        Move fullMove = findMatchingValidMove(move);
        if (fullMove == null) {
            sender.sendMessage(new Message(MessageType.INVALID_MOVE, "Invalid move"));
            return;
        }
        // Apply the move to the board
        applyMove(fullMove);
        // Check if the game is over
        checkGameOver();
        // If the game is not over, check for chain jumps or switch turns
        if (gameState.getStatus().equals("IN_PROGRESS")) {
            // If this was a jump, check if the same piece can jump again
            if (!fullMove.getCaptured().isEmpty()) {
                Position landPos = fullMove.getTo();
                PieceType landedPiece = gameState.getBoard()[landPos.getRow()][landPos.getCol()];
                List<Move> continuationJumps = findAllJumpsFromPiece(
                        gameState.getBoard(), landPos.getRow(), landPos.getCol(), landedPiece);
                if (!continuationJumps.isEmpty()) {
                    // Keep same turn, restrict to only this piece's jumps
                    gameState.setValidMoves(continuationJumps);
                    broadcastGameUpdate();
                    return;
                }
            }
            switchTurnAndBroadcast();
        } else {
            broadcastGameOver();
        }
    }

    private void switchTurnAndBroadcast() {
        // Switch turn and calculate valid moves for next turn
        String nextTurn = gameState.getCurrentTurn().equals("RED") ? "BLACK" : "RED";
        gameState.setCurrentTurn(nextTurn);
        gameState.setValidMoves(calculateValidMoves(gameState.getBoard(), nextTurn));

        // If the next player has no valid moves, they lose
        if (gameState.getValidMoves().isEmpty()) {
            if (nextTurn.equals("RED")) {
                gameState.setStatus("BLACK_WIN");
            } else {
                gameState.setStatus("RED_WIN");
            }
            broadcastGameOver();
            return;
        }

        broadcastGameUpdate();

        // If AI game and BLACK turn, let AI move
        if (isAIGame && nextTurn.equals("BLACK")) {
            handleAIMove();
        }
    }

    private void handleAIMove() {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized (GameSession.this) {
                // Ensure game is still in progress before AI makes a move
                if (!gameState.getStatus().equals("IN_PROGRESS"))
                    return;

                // Get AI move
                Move aiMove = aiPlayer.getMove(gameState);
                if (aiMove != null) {
                    // Find matching valid move for proper captured list
                    Move fullAiMove = findMatchingValidMove(aiMove);
                    if (fullAiMove == null) {
                        fullAiMove = aiMove;
                    }
                    // Apply AI move
                    applyMove(fullAiMove);
                    // Check if the game is over
                    checkGameOver();
                    // If the game is not over, switch turns and broadcast
                    if (gameState.getStatus().equals("IN_PROGRESS")) {
                        gameState.setCurrentTurn("RED");
                        gameState.setValidMoves(calculateValidMoves(gameState.getBoard(), "RED"));
                        // If human has no valid moves, AI wins
                        if (gameState.getValidMoves().isEmpty()) {
                            gameState.setStatus("BLACK_WIN");
                            broadcastGameOver();
                        } else {
                            broadcastGameUpdate();
                        }
                    } else {
                        // Broadcast game over
                        broadcastGameOver();
                    }
                }
            }
        }).start();
    }

    // Find matching valid move from list of valid moves
    private Move findMatchingValidMove(Move move) {
        for (Move validMove : gameState.getValidMoves()) {
            if (validMove.getFrom().equals(move.getFrom()) && validMove.getTo().equals(move.getTo())) {
                return validMove;
            }
        }
        return null;
    }

    private void applyMove(Move move) {
        PieceType[][] board = gameState.getBoard();
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

    // ========================================
    // GAME OVER LOGIC
    // ========================================

    private void checkGameOver() {
        PieceType[][] board = gameState.getBoard();
        boolean redExists = false;
        boolean blackExists = false;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == PieceType.RED || board[r][c] == PieceType.RED_KING) {
                    redExists = true;
                } else if (board[r][c] == PieceType.BLACK || board[r][c] == PieceType.BLACK_KING) {
                    blackExists = true;
                }
            }
        }

        if (!redExists) {
            gameState.setStatus("BLACK_WIN");
        } else if (!blackExists) {
            gameState.setStatus("RED_WIN");
        }
        // Draw: checked via no valid moves in switchTurnAndBroadcast
    }

    private void broadcastGameUpdate() {
        Message update = new Message(MessageType.GAME_UPDATE, gameState);
        redPlayer.sendMessage(update);
        if (blackPlayer != null) {
            blackPlayer.sendMessage(update);
        }
    }

    private void broadcastGameOver() {
        Message gameOver = new Message(MessageType.GAME_OVER, gameState);
        redPlayer.sendMessage(gameOver);
        if (blackPlayer != null) {
            blackPlayer.sendMessage(gameOver);
        }

        // Only record results for online PvP games
        if (!isAIGame && !isLocalGame) {
            String redName = gameState.getRedPlayer();
            String blackName = gameState.getBlackPlayer();
            if (gameState.getStatus().equals("RED_WIN")) {
                databaseManager.recordResult(redName, blackName, false);
            } else if (gameState.getStatus().equals("BLACK_WIN")) {
                databaseManager.recordResult(blackName, redName, false);
            } else if (gameState.getStatus().equals("DRAW")) {
                databaseManager.recordResult(redName, blackName, true);
            }
            // Send live stats update to both players
            sendStatsUpdate(redPlayer);
            sendStatsUpdate(blackPlayer);
        }
    }

    private void sendStatsUpdate(ClientHandler player) {
        if (player == null)
            return;
        User stats = databaseManager.getStats(player.getUsername());
        if (stats != null) {
            String payload = stats.getWins() + ":" + stats.getLosses() + ":" + stats.getDraws();
            Message statsMsg = new Message(MessageType.STATS_UPDATE, payload);
            player.sendMessage(statsMsg);
        }
    }

    // ========================================
    // VALID MOVE CALCULATION (Static — reused by AIPlayer)
    // ========================================

    public static List<Move> calculateValidMoves(PieceType[][] board, String color) {
        List<Move> jumps = new ArrayList<>();
        List<Move> simpleMoves = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                PieceType piece = board[r][c];
                if (!belongsTo(piece, color))
                    continue;

                // Find jump sequences for this piece
                List<Move> pieceJumps = findAllJumpsFromPiece(board, r, c, piece);
                jumps.addAll(pieceJumps);

                // Find simple moves for this piece
                List<Move> pieceMoves = findSimpleMoves(board, r, c, piece);
                simpleMoves.addAll(pieceMoves);
            }
        }

        // Mandatory jump rule: if any jump exists, only jumps are valid
        if (!jumps.isEmpty()) {
            return jumps;
        }
        return simpleMoves;
    }

    static boolean belongsTo(PieceType piece, String color) {
        if (color.equals("RED")) {
            return piece == PieceType.RED || piece == PieceType.RED_KING;
        } else {
            return piece == PieceType.BLACK || piece == PieceType.BLACK_KING;
        }
    }

    static boolean isOpponent(PieceType piece, String color) {
        if (color.equals("RED")) {
            return piece == PieceType.BLACK || piece == PieceType.BLACK_KING;
        } else {
            return piece == PieceType.RED || piece == PieceType.RED_KING;
        }
    }

    static String getColor(PieceType piece) {
        if (piece == PieceType.RED || piece == PieceType.RED_KING)
            return "RED";
        if (piece == PieceType.BLACK || piece == PieceType.BLACK_KING)
            return "BLACK";
        return null;
    }

    static boolean isKing(PieceType piece) {
        return piece == PieceType.RED_KING || piece == PieceType.BLACK_KING;
    }

    static int[] getRowDirections(PieceType piece) {
        if (isKing(piece)) {
            return new int[] { -1, 1 };
        }
        if (piece == PieceType.RED) {
            return new int[] { 1 }; // Red moves toward increasing rows
        }
        return new int[] { -1 }; // Black moves toward decreasing rows
    }

    private static List<Move> findSimpleMoves(PieceType[][] board, int row, int col, PieceType piece) {
        List<Move> moves = new ArrayList<>();
        int[] rowDirs = getRowDirections(piece);
        int[] colDirs = { -1, 1 };

        for (int dr : rowDirs) {
            for (int dc : colDirs) {
                int nr = row + dr;
                int nc = col + dc;
                if (inBounds(nr, nc) && board[nr][nc] == PieceType.EMPTY) {
                    moves.add(new Move(new Position(row, col), new Position(nr, nc)));
                }
            }
        }
        return moves;
    }    /**
     * Find single-step jumps from (startRow, startCol).
     * No chain — each jump captures exactly one piece.
     */
    private static List<Move> findAllJumpsFromPiece(PieceType[][] board, int startRow, int startCol, PieceType piece) {
        List<Move> results = new ArrayList<>();
        String color = getColor(piece);
        int[] rowDirs = getRowDirections(piece);
        int[] colDirs = { -1, 1 };

        for (int dr : rowDirs) {
            for (int dc : colDirs) {
                int midRow = startRow + dr;
                int midCol = startCol + dc;
                int landRow = startRow + 2 * dr;
                int landCol = startCol + 2 * dc;

                if (!inBounds(landRow, landCol))
                    continue;

                if (isOpponent(board[midRow][midCol], color)
                        && board[landRow][landCol] == PieceType.EMPTY) {
                    List<Position> captured = new ArrayList<>();
                    captured.add(new Position(midRow, midCol));
                    results.add(new Move(
                            new Position(startRow, startCol),
                            new Position(landRow, landCol),
                            captured));
                }
            }
        }
        return results;
    }

    static boolean inBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // ========================================
    // CHAT
    // ========================================

    public void forwardChat(ClientHandler sender, Message chatMessage) {
        if (isAIGame || isLocalGame)
            return;
        if (sender == redPlayer && blackPlayer != null) {
            blackPlayer.sendMessage(chatMessage);
        } else {
            redPlayer.sendMessage(chatMessage);
        }
    }

    // ========================================
    // DRAW OFFER / ACCEPT / DECLINE
    // ========================================

    public void forwardDrawOffer(ClientHandler sender) {
        Message drawOffer = new Message(MessageType.DRAW_OFFER, sender.getUsername() + " offers a draw");
        if (sender == redPlayer && blackPlayer != null) {
            blackPlayer.sendMessage(drawOffer);
        } else if (sender == blackPlayer) {
            redPlayer.sendMessage(drawOffer);
        }
    }

    public void handleDrawAccept(ClientHandler sender) {
        // Set game status to DRAW and broadcast game over to both players
        gameState.setStatus("DRAW");
        broadcastGameOver();
        gameManager.removeSession(this);
    }

    public void handleDrawDecline(ClientHandler sender) {
        // Notify the original offerer that the draw was declined
        Message decline = new Message(MessageType.DRAW_DECLINE, "Draw offer declined");
        if (sender == redPlayer && blackPlayer != null) {
            blackPlayer.sendMessage(decline);
        } else if (sender == blackPlayer) {
            redPlayer.sendMessage(decline);
        }
    }

    // ========================================
    // PLAY AGAIN / QUIT
    // ========================================

    public synchronized void handlePlayAgain(ClientHandler sender) {
        if (sender == redPlayer) {
            redWantsPlayAgain = true;
        } else if (sender == blackPlayer) {
            blackWantsPlayAgain = true;
        }

        if ((isAIGame || isLocalGame) && redWantsPlayAgain) {
            startGame();
        } else if (!isAIGame && !isLocalGame && redWantsPlayAgain && blackWantsPlayAgain) {
            startGame();
        }
    }

    public void handleQuit(ClientHandler sender) {
        Message quitMsg = new Message(MessageType.QUIT, "Opponent left the game");

        // Record forfeit result for online PvP games only
        if (!isAIGame && !isLocalGame) {
            if (sender == redPlayer && blackPlayer != null) {
                // Red quit → Black wins
                databaseManager.recordResult(blackPlayer.getUsername(), redPlayer.getUsername(), false);
                sendStatsUpdate(redPlayer);
                sendStatsUpdate(blackPlayer);
            } else if (sender == blackPlayer) {
                // Black quit → Red wins
                databaseManager.recordResult(redPlayer.getUsername(), blackPlayer.getUsername(), false);
                sendStatsUpdate(redPlayer);
                sendStatsUpdate(blackPlayer);
            }
        }

        if (sender == redPlayer && blackPlayer != null) {
            blackPlayer.sendMessage(quitMsg);
            blackPlayer.setCurrentSession(null);
        } else if (sender == blackPlayer) {
            redPlayer.sendMessage(quitMsg);
        }
        sender.setCurrentSession(null);
        if (redPlayer != null)
            redPlayer.setCurrentSession(null);
        gameManager.removeSession(this);
    }

    private String getPlayerColor(ClientHandler handler) {
        if (handler == redPlayer)
            return "RED";
        if (handler == blackPlayer)
            return "BLACK";
        return null;
    }
}
