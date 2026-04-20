import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import model.GameState;
import model.Message;
import model.Message.MessageType;
import model.Move;
import model.PieceType;
import model.Position;

public class GameController {

    private static final int CELL_SIZE = 70;

    private GuiClient app;
    private GameState gameState;
    private String myColor; // "RED" or "BLACK"

    // Board UI
    private GridPane boardGrid;
    private Pane pieceLayer;
    private StackPane boardStack;

    // Piece tracking  
    private Position selectedPiece = null;
    private List<Position> highlightedDests = new ArrayList<>();

    // Turn indicator
    private Label turnLabel;
    private Label playerInfoLabel;

    // Chat
    private TextArea chatArea;
    private TextField chatInput;
    private Button chatSendButton;

    public GameController(GuiClient app) {
        this.app = app;
    }

    public Scene createScene() {
        // ---- Board ----
        boardGrid = new GridPane();
        pieceLayer = new Pane();
        pieceLayer.setPrefSize(8 * CELL_SIZE, 8 * CELL_SIZE);
        pieceLayer.setMouseTransparent(false);

        boardStack = new StackPane(boardGrid, pieceLayer);
        boardStack.setPrefSize(8 * CELL_SIZE, 8 * CELL_SIZE);

        // ---- Status labels ----
        turnLabel = new Label("Waiting for game...");
        turnLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e0c068;");

        playerInfoLabel = new Label("");
        playerInfoLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #aaaaaa;");

        VBox statusBox = new VBox(5, turnLabel, playerInfoLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Chat panel ----
        Label chatLabel = new Label("Chat");
        chatLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e0c068;");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefSize(220, 480);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #cccccc; " +
                          "-fx-font-family: 'Consolas'; -fx-font-size: 12;");

        chatInput = new TextField();
        chatInput.setPromptText("Send a message...");
        chatInput.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: white; " +
                           "-fx-prompt-text-fill: #666666;");

        chatSendButton = new Button("Send");
        chatSendButton.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white;");

        HBox chatInputRow = new HBox(5, chatInput, chatSendButton);
        chatInput.setPrefWidth(155);

        VBox chatBox = new VBox(8, chatLabel, chatArea, chatInputRow);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #252535; -fx-background-radius: 6;");
        chatBox.setPrefWidth(240);

        chatSendButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        // ---- Layout ----
        VBox boardColumn = new VBox(10, statusBox, boardStack);
        boardColumn.setAlignment(Pos.TOP_CENTER);
        boardColumn.setPadding(new Insets(10));

        HBox root = new HBox(15, boardColumn, chatBox);
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_LEFT);

        return new Scene(root, 840, 590);
    }

    // ========================================
    // BOARD RENDERING
    // ========================================

    private void renderBoard(GameState state) {
        boardGrid.getChildren().clear();
        pieceLayer.getChildren().clear();
        highlightedDests.clear();

        PieceType[][] board = state.getBoard();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Rectangle square = new Rectangle(CELL_SIZE, CELL_SIZE);

                boolean isDark = (r + c) % 2 == 1;
                square.setFill(isDark ? Color.web("#5d3a1a") : Color.web("#f0d9b5"));
                square.setStroke(Color.web("#3a2510"));
                square.setStrokeWidth(0.5);

                boardGrid.add(square, c, r);
            }
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                PieceType piece = board[r][c];
                if (piece != PieceType.EMPTY) {
                    drawPiece(r, c, piece);
                }
            }
        }
    }

    private void drawPiece(int row, int col, PieceType piece) {
        boolean isRed = (piece == PieceType.RED || piece == PieceType.RED_KING);
        boolean isKing = (piece == PieceType.RED_KING || piece == PieceType.BLACK_KING);

        double cx = col * CELL_SIZE + CELL_SIZE / 2.0;
        double cy = row * CELL_SIZE + CELL_SIZE / 2.0;
        double radius = CELL_SIZE / 2.0 - 6;

        // Outer glow / shadow circle
        Circle shadow = new Circle(cx + 2, cy + 2, radius);
        shadow.setFill(Color.rgb(0, 0, 0, 0.35));

        // Main piece
        Circle circle = new Circle(cx, cy, radius);
        if (isRed) {
            circle.setFill(isKing ? Color.web("#c0392b") : Color.web("#e74c3c"));
            circle.setStroke(Color.web("#922b21"));
        } else {
            circle.setFill(isKing ? Color.web("#1a1a4a") : Color.web("#2c3e7a"));
            circle.setStroke(Color.web("#1a237e"));
        }
        circle.setStrokeWidth(2.5);

        pieceLayer.getChildren().addAll(shadow, circle);

        // King crown ring
        if (isKing) {
            Circle crown = new Circle(cx, cy, radius * 0.45);
            crown.setFill(Color.web("#f0c040"));
            crown.setStroke(Color.web("#b8860b"));
            crown.setStrokeWidth(1.5);
            pieceLayer.getChildren().add(crown);
        }

        // Click handler on piece
        final int finalRow = row;
        final int finalCol = col;
        circle.setOnMouseClicked(e -> handlePieceClick(finalRow, finalCol));
        if (isKing) {
            Circle kingCenter = (Circle) pieceLayer.getChildren().get(pieceLayer.getChildren().size() - 1);
            kingCenter.setOnMouseClicked(e -> handlePieceClick(finalRow, finalCol));
        }
    }

    private void highlightSquare(int row, int col) {
        Rectangle highlight = new Rectangle(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        highlight.setFill(Color.rgb(0, 220, 80, 0.45));
        highlight.setStroke(Color.web("#00ff66"));
        highlight.setStrokeWidth(2);
        highlight.setOnMouseClicked(e -> handleDestClick(row, col));
        pieceLayer.getChildren().add(highlight);
    }

    private void highlightSelected(int row, int col) {
        Rectangle sel = new Rectangle(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        sel.setFill(Color.rgb(255, 200, 0, 0.35));
        sel.setStroke(Color.web("#ffcc00"));
        sel.setStrokeWidth(2.5);
        sel.setMouseTransparent(true);
        pieceLayer.getChildren().add(sel);
    }

    // ========================================
    // CLICK HANDLING
    // ========================================

    private void handlePieceClick(int row, int col) {
        if (gameState == null) return;
        if (!isMyTurn()) return; // ignore clicks when not your turn

        PieceType piece = gameState.getBoard()[row][col];
        String myColorStr = myColor;

        // Check clicked piece belongs to this player
        boolean ownsPiece = myColorStr.equals("RED")
                ? (piece == PieceType.RED || piece == PieceType.RED_KING)
                : (piece == PieceType.BLACK || piece == PieceType.BLACK_KING);

        if (!ownsPiece) {
            // Maybe clicking a highlighted destination
            if (highlightedDests.contains(new Position(row, col))) {
                handleDestClick(row, col);
            }
            return;
        }

        // Check if any valid moves start from this piece
        List<Move> validMoves = gameState.getValidMoves();
        List<Position> dests = new ArrayList<>();
        for (Move m : validMoves) {
            if (m.getFrom().getRow() == row && m.getFrom().getCol() == col) {
                dests.add(m.getTo());
            }
        }

        if (dests.isEmpty()) return; // no moves from this piece

        // Re-render to clear old highlights, then apply new ones
        renderBoard(gameState);
        selectedPiece = new Position(row, col);
        highlightedDests = dests;

        highlightSelected(row, col);
        for (Position dest : dests) {
            highlightSquare(dest.getRow(), dest.getCol());
        }
    }

    private void handleDestClick(int row, int col) {
        if (selectedPiece == null) return;

        Move move = new Move(selectedPiece, new Position(row, col));
        app.send(new Message(MessageType.MOVE, move));

        selectedPiece = null;
        highlightedDests.clear();
    }

    private boolean isMyTurn() {
        return gameState != null && myColor != null && myColor.equals(gameState.getCurrentTurn());
    }

    // ========================================
    // MESSAGE HANDLERS (called from GuiClient)
    // ========================================

    public void onGameStart(Message message) {
        gameState = message.getGameState();
        myColor = gameState.getRedPlayer().equals(app.getUsername()) ? "RED" : "BLACK";
        updateTurnLabel();
        renderBoard(gameState);
        playerInfoLabel.setText("You are " + myColor + "  |  Red: " + gameState.getRedPlayer() +
                                "  vs  Black: " + gameState.getBlackPlayer());
        chatArea.clear();
    }

    public void onGameUpdate(Message message) {
        gameState = message.getGameState();
        updateTurnLabel();
        renderBoard(gameState);
    }

    public void onInvalidMove(Message message) {
        selectedPiece = null;
        highlightedDests.clear();
        if (gameState != null) renderBoard(gameState);
        chatArea.appendText("[System] Invalid move: " + message.getContent() + "\n");
    }

    public void onGameOver(Message message) {
        gameState = message.getGameState();
        renderBoard(gameState);

        String status = gameState.getStatus();
        String resultText;
        if (status.equals("RED_WIN")) {
            resultText = myColor.equals("RED") ? "You won! \uD83C\uDFC6" : "You lost. Better luck next time!";
        } else if (status.equals("BLACK_WIN")) {
            resultText = myColor.equals("BLACK") ? "You won! \uD83C\uDFC6" : "You lost. Better luck next time!";
        } else {
            resultText = "It's a draw!";
        }

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Game Over");
        alert.setHeaderText(resultText);
        alert.setContentText("What would you like to do?");

        ButtonType playAgainBtn = new ButtonType("Play Again");
        ButtonType quitBtn = new ButtonType("Quit");
        alert.getButtonTypes().setAll(playAgainBtn, quitBtn);

        alert.showAndWait().ifPresent(choice -> {
            if (choice == playAgainBtn) {
                app.send(new Message(MessageType.PLAY_AGAIN));
            } else {
                app.send(new Message(MessageType.QUIT));
                app.switchToScene("lobby");
            }
        });
    }

    public void onChat(Message message) {
        String sender = message.getSender() != null ? message.getSender() : "Opponent";
        chatArea.appendText(sender + ": " + message.getContent() + "\n");
    }

    public void onOpponentQuit(Message message) {
        chatArea.appendText("[System] Opponent left the game.\n");
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your opponent left the game.", ButtonType.OK);
        alert.setTitle("Opponent Disconnected");
        alert.showAndWait();
    }

    // ========================================
    // HELPERS
    // ========================================

    private void updateTurnLabel() {
        if (gameState == null) return;
        if (isMyTurn()) {
            turnLabel.setText("Your turn (" + myColor + ")");
            turnLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        } else {
            String other = myColor.equals("RED") ? "BLACK" : "RED";
            turnLabel.setText("Waiting for opponent's move... (" + other + ")");
            turnLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            Message chatMsg = new Message(MessageType.CHAT);
            chatMsg.setSender(app.getUsername());
            chatMsg.setContent(text);
            app.send(chatMsg);
            chatArea.appendText("You: " + text + "\n");
            chatInput.clear();
        }
    }
}
