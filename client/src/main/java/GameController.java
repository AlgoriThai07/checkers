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
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import model.GameState;
import model.Message;
import model.Message.MessageType;
import model.Move;
import model.PieceType;
import model.Position;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class GameController {

    private GuiClient app;
    private GameState gameState;
    private String myColor; // "RED" or "BLACK"
    private boolean isLocalGame = false;

    // Component tracking
    private GridPane boardGrid;
    private StackPane[][] boardCells = new StackPane[8][8];
    private StackPane boardContainer;

    // Piece tracking
    private Position selectedPiece = null;
    private List<Position> highlightedDests = new ArrayList<>();

    // UI Tracking
    private ListView<String> chatList;
    private TextField chatInput;

    private Label statusLabel;
    private Label p1NameLabel;
    private Label p2NameLabel;
    private Label p1CapturedLabel;
    private Label p2CapturedLabel;

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

        // Right: Chat and controls
        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        // Center: Game board (passing root and side panel for responsive square
        // binding)
        VBox centerContent = createBoardSection(root, rightPanel);
        root.setCenter(centerContent);

        return root;
    }

    private HBox createPlayerInfo(boolean isPlayer1) {
        HBox playerInfo = new HBox(15);
        playerInfo.setPadding(new Insets(15, 25, 15, 25));
        playerInfo.setAlignment(Pos.CENTER_LEFT);
        playerInfo.getStyleClass().add("opponent-player-info");

        // Player name
        String displayColor = isPlayer1 ? "(Green)" : "(Black)";
        Label nameLabel = new Label(isPlayer1 ? "Player 1 " + displayColor : "Player 2 " + displayColor);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.getStyleClass().add(isPlayer1 ? "username-label" : "title");

        if (isPlayer1)
            p1NameLabel = nameLabel;
        else
            p2NameLabel = nameLabel;

        // Captured pieces count
        Label capturedLabel = new Label("Captured: 0");
        capturedLabel.setFont(Font.font("System", 13));
        capturedLabel.getStyleClass().add("secondary-text");

        if (isPlayer1)
            p1CapturedLabel = capturedLabel;
        else
            p2CapturedLabel = capturedLabel;

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        playerInfo.getChildren().addAll(nameLabel, capturedLabel, spacer);

        return playerInfo;
    }

    private VBox createBoardSection(BorderPane root, VBox rightPanel) {
        VBox boardSection = new VBox(15);
        boardSection.setPadding(new Insets(10, 20, 10, 20));
        boardSection.setAlignment(Pos.CENTER);

        // Status label (Turn indicator) - Moved to bottom of board section
        statusLabel = new Label("Waiting for match...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setPadding(new Insets(12));

        // Player 2 info (opponent - top)
        HBox player2Info = createPlayerInfo(false);

        // Board container to maintain square aspect ratio
        boardContainer = new StackPane();
        boardContainer.getStyleClass().add("board-container");

        // Fix board sizing: bind to Parent BorderPane size minus the side panel and
        // top/bottom padding
        // This avoids the 0-size initialization issue
        boardContainer.maxWidthProperty().bind(Bindings.min(
                root.widthProperty().subtract(380), // Space for right panel + padding
                root.heightProperty().subtract(260) // Space for top bar + bottom labels + padding
        ));
        boardContainer.setPrefSize(400, 400); // Set a reasonable initial size to prevent jumping
        boardContainer.maxHeightProperty().bind(boardContainer.maxWidthProperty());
        boardContainer.minWidthProperty().bind(boardContainer.maxWidthProperty());
        boardContainer.minHeightProperty().bind(boardContainer.maxHeightProperty());

        // Create 8x8 board
        boardGrid = createBoard();
        boardContainer.getChildren().add(boardGrid);

        // Player 1 info (current player - bottom)
        HBox player1Info = createPlayerInfo(true);

        boardSection.getChildren().addAll(player2Info, boardContainer, player1Info, statusLabel);

        return boardSection;
    }

    private GridPane createBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("checkerboard");
        grid.maxWidthProperty().bind(boardContainer.maxWidthProperty());
        grid.maxHeightProperty().bind(boardContainer.maxHeightProperty());

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
        boolean isGreen = (pt == PieceType.RED || pt == PieceType.RED_KING);
        boolean isKing = (pt == PieceType.RED_KING || pt == PieceType.BLACK_KING);

        Circle piece = new Circle(25);
        piece.radiusProperty().bind(boardGrid.widthProperty().divide(20)); // Responsive piece size
        piece.getStyleClass().add(isGreen ? "player1-piece" : "player2-piece");

        if (isGreen) {
            piece.setFill(Color.web("#2ecc71")); // Green
            piece.setStroke(Color.web("#1a9c54"));
        } else {
            piece.setFill(Color.web("#9b59b6")); // Purple
            piece.setStroke(Color.web("#6c3483"));
        }
        piece.setStrokeWidth(3);

        if (isKing) {
            piece.setStrokeWidth(5);
            piece.setStroke(Color.web("#FFD166")); // Neon Gold marker for King
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
        VBox controlsBox = new VBox(15);
        controlsBox.setPadding(new Insets(10, 0, 0, 0));

        Button offerDrawButton = new Button("Offer Draw");
        offerDrawButton.setPrefHeight(60);
        offerDrawButton.setMaxWidth(Double.MAX_VALUE);
        offerDrawButton.getStyleClass().add("secondary-button");
        offerDrawButton.setStyle(offerDrawButton.getStyle() + "-fx-font-size: 16px; -fx-font-weight: bold;");
        offerDrawButton.setOnAction(e -> handleOfferDraw());

        Button resignButton = new Button("Resign");
        resignButton.setPrefHeight(60);
        resignButton.setMaxWidth(Double.MAX_VALUE);
        resignButton.getStyleClass().add("resign-button");
        resignButton.setStyle(resignButton.getStyle() + "-fx-font-size: 16px; -fx-font-weight: bold;");
        resignButton.setOnAction(e -> handleResign());

        controlsBox.getChildren().addAll(offerDrawButton, resignButton);

        rightPanel.getChildren().addAll(
                chatLabel,
                chatList,
                chatInputBox,
                controlsBox);

        return rightPanel;
    }

    // ========================================
    // BOARD RENDERING & CLICK HANDLING
    // ========================================

    private int getPhysicalRow(int logicalRow) {
        // Red is logical 0-2, Black is logical 5-7.
        // We want our color at physical 5-7 (bottom).
        if (myColor != null && myColor.equals("RED")) {
            return 7 - logicalRow; // Flip Red (0) to Bottom (7)
        }
        return logicalRow; // Keep Black (7) at Bottom (7)
    }

    private int getPhysicalCol(int logicalCol) {
        if (myColor != null && myColor.equals("RED")) {
            return 7 - logicalCol;
        }
        return logicalCol;
    }

    private int getLogicalRow(int physicalRow) {
        if (myColor != null && myColor.equals("RED")) {
            return 7 - physicalRow;
        }
        return physicalRow;
    }

    private int getLogicalCol(int physicalCol) {
        if (myColor != null && myColor.equals("RED")) {
            return 7 - physicalCol;
        }
        return physicalCol;
    }

    private void renderBoard(GameState state) {
        if (boardGrid == null)
            return;
        highlightedDests.clear();

        PieceType[][] board = state.getBoard();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int physR = getPhysicalRow(r);
                int physC = getPhysicalCol(c);
                StackPane cell = boardCells[physR][physC];
                cell.getChildren().clear(); // Clear existing piece or highlight

                PieceType piece = board[r][c];
                if (piece != PieceType.EMPTY) {
                    Circle pieceCircle = createPiece(piece);
                    cell.getChildren().add(pieceCircle);
                }
            }
        }
        updateCapturedCounts(state);
    }

    private void updateCapturedCounts(GameState state) {
        if (state == null) return;
        PieceType[][] board = state.getBoard();
        int redCount = 0;
        int blackCount = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == PieceType.RED || board[r][c] == PieceType.RED_KING) redCount++;
                if (board[r][c] == PieceType.BLACK || board[r][c] == PieceType.BLACK_KING) blackCount++;
            }
        }
        // Each player starts with 12 pieces
        int redCaptured = 12 - blackCount;  // Red captured this many black pieces
        int blackCaptured = 12 - redCount;  // Black captured this many red pieces

        // p1 is always "me" (bottom), p2 is opponent (top)
        if (myColor != null && myColor.equals("RED")) {
            if (p1CapturedLabel != null) p1CapturedLabel.setText("Captured: " + redCaptured);
            if (p2CapturedLabel != null) p2CapturedLabel.setText("Captured: " + blackCaptured);
        } else {
            if (p1CapturedLabel != null) p1CapturedLabel.setText("Captured: " + blackCaptured);
            if (p2CapturedLabel != null) p2CapturedLabel.setText("Captured: " + redCaptured);
        }
    }

    private void highlightSquare(int row, int col) {
        int physR = getPhysicalRow(row);
        int physC = getPhysicalCol(col);
        Rectangle highlight = new Rectangle();
        highlight.setFill(Color.rgb(129, 182, 76, 0.45)); // lime green
        highlight.setStroke(Color.web("#81b64c"));
        highlight.setStrokeWidth(2);
        highlight.setStrokeType(StrokeType.INSIDE);
        highlight.widthProperty().bind(boardCells[physR][physC].widthProperty());
        highlight.heightProperty().bind(boardCells[physR][physC].heightProperty());
        highlight.setMouseTransparent(true); // Don't steal cell clicks
        boardCells[physR][physC].getChildren().add(highlight);
    }

    private void highlightSelected(int row, int col) {
        int physR = getPhysicalRow(row);
        int physC = getPhysicalCol(col);
        Rectangle sel = new Rectangle();
        sel.setFill(Color.rgb(255, 200, 0, 0.35));
        sel.setStroke(Color.web("#ffcc00"));
        sel.setStrokeWidth(3.0);
        sel.setStrokeType(StrokeType.INSIDE);
        sel.setMouseTransparent(true);
        sel.widthProperty().bind(boardCells[physR][physC].widthProperty());
        sel.heightProperty().bind(boardCells[physR][physC].heightProperty());
        boardCells[physR][physC].getChildren().add(sel);
    }

    private void handleCellClick(int physRow, int physCol) {
        if (gameState == null)
            return;
        if (!isMyTurn())
            return; // ignore clicks when not your turn

        int row = getLogicalRow(physRow);
        int col = getLogicalCol(physCol);

        PieceType piece = gameState.getBoard()[row][col];
        // In local mode, use the current turn's color for ownership check
        String activeColor = isLocalGame ? gameState.getCurrentTurn() : myColor;

        // Check clicked piece belongs to the active player
        boolean ownsPiece = activeColor.equals("RED")
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

        if (dests.isEmpty())
            return; // no moves from this piece

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
        if (selectedPiece == null)
            return;

        Move move = new Move(selectedPiece, new Position(row, col));
        app.send(new Message(MessageType.MOVE, move));

        selectedPiece = null;
        highlightedDests.clear();
    }

    private boolean isMyTurn() {
        if (isLocalGame) return gameState != null;
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
        if (isLocalGame) {
            showCustomModal(
                "Draw",
                "It's a draw!",
                true,
                "↻ Play Again",
                "← Lobby",
                () -> app.send(new Message(MessageType.PLAY_AGAIN)),
                () -> {
                    app.send(new Message(MessageType.QUIT));
                    app.switchToScene("lobby");
                }
            );
            return;
        }
        if (gameState != null && (gameState.getBlackPlayer().equals("AI") || gameState.getRedPlayer().equals("AI"))) {
            showCustomModal(
                "Draw Accepted",
                "The AI has accepted your draw offer.",
                true,
                "↻ Play Again",
                "← Lobby",
                () -> app.send(new Message(MessageType.PLAY_AGAIN)),
                () -> {
                    app.send(new Message(MessageType.QUIT));
                    app.switchToScene("lobby");
                }
            );
        } else {
            // Online PvP: send draw offer to opponent via server
            app.send(new Message(MessageType.DRAW_OFFER));
            chatList.getItems().add("[System] Draw offer sent to opponent...");
        }
    }

    public void onDrawOffer(Message message) {
        // Opponent is offering a draw — show accept/decline modal
        showCustomModal(
            "Draw Offer",
            "Your opponent is offering a draw.",
            false,
            "✓ Accept",
            "✗ Decline",
            () -> app.send(new Message(MessageType.DRAW_ACCEPT)),
            () -> app.send(new Message(MessageType.DRAW_DECLINE))
        );
    }

    public void onDrawDecline(Message message) {
        // Our draw offer was declined — notify and continue playing
        chatList.getItems().add("[System] Opponent declined your draw offer.");
    }

    private void handleResign() {
        showCustomModal(
            "Resign",
            "Are you sure you want to forfeit?",
            false,
            "Yes, Resign",
            "Cancel",
            () -> {
                app.send(new Message(MessageType.QUIT));
                app.switchToScene("lobby");
            },
            () -> { /* Do nothing on cancel */ }
        );
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
        isLocalGame = "Opponent".equals(gameState.getBlackPlayer());
        myColor = gameState.getRedPlayer().equals(app.getUsername()) ? "RED" : "BLACK";

        String myDisplayColor = myColor.equals("RED") ? "Green" : "Black";
        String opponentDisplayColor = myColor.equals("RED") ? "Black" : "Green";

        // Assign labels so that Player 1 (Bottom Label) is always US.
        if (myColor.equals("RED")) {
            // We are Red (Green), so we are at the bottom. Opponent is Black, at the top.
            p1NameLabel.setText(app.getUsername() + " (Green)");
            p2NameLabel.setText(gameState.getBlackPlayer() + " (Black)");
        } else {
            // We are Black, so we are at the bottom. Opponent is Red (Green), at the top.
            p1NameLabel.setText(app.getUsername() + " (Black)");
            p2NameLabel.setText(gameState.getRedPlayer() + " (Green)");
        }



        updateTurnLabel();
        renderBoard(gameState);

        chatList.getItems().clear();
        chatList.getItems().add("[System] Game started! You are " + myDisplayColor);
    }

    public void onGameUpdate(Message message) {
        gameState = message.getGameState();
        updateTurnLabel();
        renderBoard(gameState);
    }

    public void onInvalidMove(Message message) {
        selectedPiece = null;
        highlightedDests.clear();
        if (gameState != null)
            renderBoard(gameState);
        chatList.getItems().add("[System] Invalid move: " + message.getContent());
    }

    public void onGameOver(Message message) {
        gameState = message.getGameState();
        renderBoard(gameState);

        String status = gameState.getStatus();
        String resultText;
        boolean isWin = false;

        if (status.equals("RED_WIN")) {
            isWin = myColor.equals("RED");
            resultText = isWin ? "You won! \uD83C\uDFC6" : "Opponent wins! Better luck next time.";
        } else if (status.equals("BLACK_WIN")) {
            isWin = myColor.equals("BLACK");
            resultText = isWin ? "You won! \uD83C\uDFC6" : "Opponent wins! Better luck next time.";
        } else {
            resultText = "It's a draw!";
        }

        showCustomModal(
            "Game Over",
            resultText,
            isWin,
            "↻ Play Again",
            "← Lobby",
            () -> app.send(new Message(MessageType.PLAY_AGAIN)),
            () -> {
                app.send(new Message(MessageType.QUIT));
                app.switchToScene("lobby");
            }
        );
    }

    private void showCustomModal(String title, String subtitle, boolean isGoldIcon, String leftBtnText, String rightBtnText, Runnable onLeftClick, Runnable onRightClick) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        // Find main window
        if (boardContainer != null && boardContainer.getScene() != null) {
            dialogStage.initOwner(boardContainer.getScene().getWindow());
        }
        
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox dialogRoot = new VBox(20);
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setPadding(new Insets(40, 50, 40, 50));
        dialogRoot.setStyle("-fx-background-color: #111419; -fx-background-radius: 12; -fx-border-color: rgba(0,240,255,0.3); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.2), 25, 0, 0, 0);");

        // Icon
        Label iconLabel = new Label(isGoldIcon ? "★" : "⚑");
        iconLabel.setStyle("-fx-font-size: 48; -fx-text-fill: #FFD166; -fx-background-color: #1a2030; -fx-background-radius: 50; -fx-padding: 20 28 20 28;");
        iconLabel.setAlignment(Pos.CENTER);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.web("#e6eef6"));

        // Subtitle
        Label subLabel = new Label(subtitle);
        subLabel.setFont(Font.font("System", 16));
        subLabel.setTextFill(Color.web("#9aa6b2"));

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button leftBtn = new Button(leftBtnText);
        leftBtn.setStyle("-fx-background-color: #00f0ff; -fx-text-fill: #0b0f14; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.3), 10, 0, 0, 0);");
        leftBtn.setOnAction(e -> {
            dialogStage.close();
            onLeftClick.run();
        });

        Button rightBtn = new Button(rightBtnText);
        rightBtn.setStyle("-fx-background-color: #1a1f26; -fx-text-fill: #e6eef6; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8; -fx-border-width: 1;");
        rightBtn.setOnAction(e -> {
            dialogStage.close();
            onRightClick.run();
        });

        buttonBox.getChildren().addAll(leftBtn, rightBtn);
        dialogRoot.getChildren().addAll(iconLabel, titleLabel, subLabel, buttonBox);

        Scene dialogScene = new Scene(dialogRoot);
        dialogScene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
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
        if (gameState == null)
            return;
        if (isLocalGame) {
            String turnColor = gameState.getCurrentTurn();
            String colorName = turnColor.equals("RED") ? "Green" : "Black";
            statusLabel.setText(colorName + "'s turn");
            statusLabel.setStyle("-fx-text-fill: #1a1613; -fx-background-color: #81b64c; -fx-background-radius: 4;");
        } else if (isMyTurn()) {
            String colorName = myColor.equals("RED") ? "Green" : "Black";
            statusLabel.setText("Your turn (" + colorName + ")");
            statusLabel.setStyle("-fx-text-fill: #1a1613; -fx-background-color: #81b64c; -fx-background-radius: 4;");
        } else {
            String other = myColor.equals("RED") ? "BLACK" : "RED";
            String colorName = other.equals("RED") ? "Green" : "Black";
            statusLabel.setText("Opponent's turn (" + colorName + ")");
            statusLabel.setStyle("-fx-text-fill: #e8e6e3; -fx-background-color: #3d3935; -fx-background-radius: 4;");
        }
    }
}
