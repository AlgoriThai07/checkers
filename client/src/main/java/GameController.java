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
        if (myColor != null && myColor.equals("BLACK")) {
            return 7 - physicalRow;
        }
        return physicalRow;
    }

    private int getLogicalCol(int physicalCol) {
        if (myColor != null && myColor.equals("BLACK")) {
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
    }

    private void highlightSquare(int row, int col) {
        int physR = getPhysicalRow(row);
        int physC = getPhysicalCol(col);
        Rectangle highlight = new Rectangle();
        highlight.setFill(Color.rgb(129, 182, 76, 0.45)); // lime green
        highlight.setStroke(Color.web("#81b64c"));
        highlight.setStrokeWidth(2);
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Offer draw sent to opponent.", ButtonType.OK);
            alert.showAndWait();
        }
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
        dialogRoot.setStyle("-fx-background-color: #262421; -fx-background-radius: 12; -fx-border-color: #3d3935; -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 10);");

        // Icon
        Label iconLabel = new Label(isGoldIcon ? "\uD83C\uDFC6" : "\uD83C\uDFC1"); // Trophy or Flag
        iconLabel.setStyle("-fx-font-size: 32; -fx-text-fill: #e6a845; -fx-background-color: #3e2b21; -fx-background-radius: 50; -fx-padding: 15;");

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.WHITE);

        // Subtitle
        Label subLabel = new Label(subtitle);
        subLabel.setFont(Font.font("System", 16));
        subLabel.setTextFill(Color.web("#c7c4c0"));

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button leftBtn = new Button(leftBtnText);
        leftBtn.setStyle("-fx-background-color: #81b64c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        leftBtn.setOnAction(e -> {
            dialogStage.close();
            onLeftClick.run();
        });

        Button rightBtn = new Button(rightBtnText);
        rightBtn.setStyle("-fx-background-color: #3d3935; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
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
        if (isMyTurn()) {
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
