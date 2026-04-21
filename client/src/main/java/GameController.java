import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import model.GameState;
import model.Message;
import model.Message.MessageType;
import model.Move;
import model.PieceType;
import model.Position;

public class GameController {

    private GuiClient app;
    private GameState gameState;
    private String myColor; // "RED" or "BLACK"

    // Component tracking
    private GridPane boardGrid;
    private StackPane[][] boardCells = new StackPane[8][8];
    
    // Piece tracking  
    private Position selectedPiece = null;
    private List<Position> highlightedDests = new ArrayList<>();

    // UI Tracking
    private ListView<String> chatList;
    private TextField chatInput;
    private Label player1TimeLabel;
    private Label player2TimeLabel;
    private Label statusLabel;
    private Label p1NameLabel;
    private Label p2NameLabel;

    public GameController(GuiClient app) {
        this.app = app;
    }

    public Scene createScene() {
        BorderPane root = createGameLayout();

        Scene scene = new Scene(root, 1200, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/app-styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load app-styles.css");
        }

        return scene;
    }

    private BorderPane createGameLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar with player info
        VBox topSection = createTopSection();
        root.setTop(topSection);

        // Center: Game board
        VBox centerContent = createBoardSection();
        root.setCenter(centerContent);

        // Right: Chat and controls
        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        return root;
    }

    private VBox createTopSection() {
        VBox topSection = new VBox();
        topSection.getStyleClass().add("top-bar");

        // Player 2 info (opponent - top) - RED forms top
        HBox player2Info = createPlayerInfo(false);

        // Status label
        statusLabel = new Label("Waiting for match...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setPadding(new Insets(10));

        topSection.getChildren().addAll(player2Info, statusLabel);

        return topSection;
    }

    private HBox createPlayerInfo(boolean isPlayer1) {
        HBox playerInfo = new HBox(15);
        playerInfo.setPadding(new Insets(15, 25, 15, 25));
        playerInfo.setAlignment(Pos.CENTER_LEFT);
        playerInfo.getStyleClass().add("opponent-player-info");

        // Player name
        Label nameLabel = new Label(isPlayer1 ? "Player 1 (Black)" : "Player 2 (Red)");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.getStyleClass().add(isPlayer1 ? "username-label" : "title");
        
        if (isPlayer1) p1NameLabel = nameLabel;
        else p2NameLabel = nameLabel;

        // Captured pieces count
        Label capturedLabel = new Label("Captured: 0");
        capturedLabel.setFont(Font.font("System", 13));
        capturedLabel.getStyleClass().add("secondary-text");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Timer
        Label timeLabel = new Label("5:00");
        if (isPlayer1) {
            player1TimeLabel = timeLabel;
        } else {
            player2TimeLabel = timeLabel;
        }
        
        timeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        timeLabel.getStyleClass().add("timer-label");

        playerInfo.getChildren().addAll(nameLabel, capturedLabel, spacer, timeLabel);

        return playerInfo;
    }

    private VBox createBoardSection() {
        VBox boardSection = new VBox(20);
        boardSection.setPadding(new Insets(20, 20, 20, 20));
        boardSection.setAlignment(Pos.CENTER);

        // Board container to maintain square aspect ratio
        StackPane boardContainer = new StackPane();
        boardContainer.getStyleClass().add("board-container");
        boardContainer.setMaxSize(600, 600);

        // Create 8x8 board
        boardGrid = createBoard();
        boardContainer.getChildren().add(boardGrid);

        // Player 1 info (current player - bottom)
        HBox player1Info = createPlayerInfo(true);

        boardSection.getChildren().addAll(boardContainer, player1Info);

        return boardSection;
    }

    private GridPane createBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("checkerboard");
        grid.setMaxSize(600, 600);
        grid.setPrefSize(600, 600);

        // Create 8x8 grid
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                StackPane cell = createBoardCell(row, col);
                boardCells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        // Set column and row constraints for equal sizing
        for (int i = 0; i < 8; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(12.5);
            colConstraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(colConstraints);

            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(12.5);
            rowConstraints.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rowConstraints);
        }

        return grid;
    }

    private StackPane createBoardCell(int row, int col) {
        StackPane cell = new StackPane();
        cell.setMinSize(50, 50);

        // Checkerboard pattern
        boolean isDark = (row + col) % 2 == 1;
        cell.getStyleClass().add(isDark ? "dark-cell" : "light-cell");

        // Click handler for moves
        cell.setOnMouseClicked(e -> handleCellClick(row, col));

        return cell;
    }

    private Circle createPiece(PieceType pt) {
        boolean isRed = (pt == PieceType.RED || pt == PieceType.RED_KING);
        boolean isKing = (pt == PieceType.RED_KING || pt == PieceType.BLACK_KING);

        Circle piece = new Circle(25);
        piece.getStyleClass().add(isRed ? "player1-piece" : "player2-piece");

        if (isRed) {
            piece.setFill(Color.web("#81b64c")); // Lime
            piece.setStroke(Color.web("#5b7a3a"));
        } else {
            piece.setFill(Color.web("#262421")); // Dark Slate
            piece.setStroke(Color.web("#151311"));
        }
        piece.setStrokeWidth(3);

        if (isKing) {
            piece.setStrokeWidth(5);
            piece.setStroke(Color.web("#f0c040")); // Gold marker for King
        }

        return piece;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPrefWidth(320);
        rightPanel.setMinWidth(280);

        // Chat section
        Label chatLabel = new Label("Chat");
        chatLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        chatLabel.getStyleClass().add("section-label");

        // Chat messages list
        chatList = new ListView<>();
        chatList.getStyleClass().add("chat-list");
        VBox.setVgrow(chatList, Priority.ALWAYS);

        // Chat input
        HBox chatInputBox = new HBox(8);
        chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setPrefHeight(35);
        chatInput.getStyleClass().add("text-field");
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setPrefHeight(35);
        sendButton.getStyleClass().add("secondary-button");
        sendButton.setOnAction(e -> handleSendMessage());

        chatInput.setOnAction(e -> handleSendMessage());

        chatInputBox.getChildren().addAll(chatInput, sendButton);

        // Game controls section
        Label controlsLabel = new Label("Game Controls");
        controlsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        controlsLabel.getStyleClass().add("section-label");
        controlsLabel.setPadding(new Insets(15, 0, 0, 0));

        // Control buttons
        VBox controlsBox = new VBox(10);

        Button offerDrawButton = new Button("Offer Draw");
        offerDrawButton.setPrefHeight(40);
        offerDrawButton.setMaxWidth(Double.MAX_VALUE);
        offerDrawButton.getStyleClass().add("secondary-button");
        offerDrawButton.setOnAction(e -> handleOfferDraw());

        Button resignButton = new Button("Resign");
        resignButton.setPrefHeight(40);
        resignButton.setMaxWidth(Double.MAX_VALUE);
        resignButton.getStyleClass().add("resign-button");
        resignButton.setOnAction(e -> handleResign());

        Button exitButton = new Button("Exit Game");
        exitButton.setPrefHeight(40);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.getStyleClass().add("secondary-button");
        exitButton.setOnAction(e -> handleExit());

        controlsBox.getChildren().addAll(offerDrawButton, resignButton, exitButton);

        rightPanel.getChildren().addAll(
            chatLabel,
            chatList,
            chatInputBox,
            controlsLabel,
            controlsBox
        );

        return rightPanel;
    }

    // ========================================
    // BOARD RENDERING & CLICK HANDLING
    // ========================================

    private void renderBoard(GameState state) {
        if (boardGrid == null) return;
        highlightedDests.clear();

        PieceType[][] board = state.getBoard();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                StackPane cell = boardCells[r][c];
                cell.getChildren().clear(); // Clear existing piece or highlight

                PieceType piece = board[r][c];
                if (piece != PieceType.EMPTY) {
                   Circle pieceCircle = createPiece(piece);
                   cell.getChildren().add(pieceCircle);
                }
            }
        }
    }

    private void highlightSquare(int row, int col) {
        Rectangle highlight = new Rectangle();
        highlight.setFill(Color.rgb(129, 182, 76, 0.45)); // lime green
        highlight.setStroke(Color.web("#81b64c"));
        highlight.setStrokeWidth(2);
        highlight.widthProperty().bind(boardCells[row][col].widthProperty());
        highlight.heightProperty().bind(boardCells[row][col].heightProperty());
        highlight.setMouseTransparent(true); // Don't steal cell clicks
        boardCells[row][col].getChildren().add(highlight);
    }

    private void highlightSelected(int row, int col) {
        Rectangle sel = new Rectangle();
        sel.setFill(Color.rgb(255, 200, 0, 0.35));
        sel.setStroke(Color.web("#ffcc00"));
        sel.setStrokeWidth(3.0);
        sel.setMouseTransparent(true);
        sel.widthProperty().bind(boardCells[row][col].widthProperty());
        sel.heightProperty().bind(boardCells[row][col].heightProperty());
        boardCells[row][col].getChildren().add(sel);
    }

    private void handleCellClick(int row, int col) {
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

    private void handleSendMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            Message chatMsg = new Message(MessageType.CHAT);
            chatMsg.setSender(app.getUsername());
            chatMsg.setContent(text);
            app.send(chatMsg);
            
            chatList.getItems().add("You: " + text);
            chatList.scrollTo(chatList.getItems().size() - 1);
            chatInput.clear();
        }
    }

    private void handleOfferDraw() {
        System.out.println("Offer draw clicked");
    }

    private void handleResign() {
        System.out.println("Resign clicked");
    }

    private void handleExit() {
        System.out.println("Exit game clicked");
        app.send(new Message(MessageType.QUIT));
        app.switchToScene("lobby");
    }

    // ========================================
    // MESSAGE HANDLERS (called from GuiClient)
    // ========================================

    public void onGameStart(Message message) {
        gameState = message.getGameState();
        myColor = gameState.getRedPlayer().equals(app.getUsername()) ? "RED" : "BLACK";
        
        p1NameLabel.setText(gameState.getBlackPlayer() + " (Black)"); // Black is Row 7
        p2NameLabel.setText(gameState.getRedPlayer() + " (Red)");   // Red is Row 0

        updateTurnLabel();
        renderBoard(gameState);
        
        chatList.getItems().clear();
        chatList.getItems().add("[System] Game started! You are " + myColor);
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
        chatList.getItems().add("[System] Invalid move: " + message.getContent());
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
        chatList.getItems().add(sender + ": " + message.getContent());
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    public void onOpponentQuit(Message message) {
        chatList.getItems().add("[System] Opponent left the game.");
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
            statusLabel.setText("Your turn (" + myColor + ")");
            statusLabel.setStyle("-fx-text-fill: #1a1613; -fx-background-color: #81b64c; -fx-background-radius: 4;");
        } else {
            String other = myColor.equals("RED") ? "BLACK" : "RED";
            statusLabel.setText("Opponent's turn (" + other + ")");
            statusLabel.setStyle("-fx-text-fill: #e8e6e3; -fx-background-color: #3d3935; -fx-background-radius: 4;");
        }
    }
}
