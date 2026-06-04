package gamehub.gamehub;

import gamehub.games.connectFour.*;
import gamehub.network.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class Connect4Controller {

    // LAN mode — null in local mode
    private Connect4Player myColor  = null;   // set after PLAYER_ASSIGN / host setup
    private boolean        isLan    = false;
    private boolean        isHost   = false;
    private boolean        restartPending = false;
    private NetworkServer  server   = null;
    private NetworkClient  client   = null;

    private static final int PORT = 55501;


    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color COLOR_EMPTY     = Color.web("#0d1b2e");
    private static final Color COLOR_EMPTY_RIM = Color.web("#1a2a40");
    private static final Color COLOR_RED       = Color.web("#e74c3c");
    private static final Color COLOR_YELLOW    = Color.web("#f1c40f");
    private static final Color COLOR_WIN_LINE  = Color.web("#f5c842");
    private static final Color COLOR_DIM       = Color.web("#ffffff", 0.18);

    private static final double DISC_RADIUS  = 30.0;
    private static final double CELL_SIZE    = 80.0;
    private static final double BOARD_PAD    = 8.0;

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ScrollPane gamePane;
    @FXML private VBox       lobbyPane;
    @FXML private Label      titleLabel;
    @FXML private Label      lobbyStatus;
    @FXML private Button     hostBtn;
    @FXML private Button     joinBtn;
    @FXML private HBox       ipBox;
    @FXML private TextField  ipField;
    @FXML private Button     connectBtn;
    @FXML private Button     restartBtn;

    @FXML private Pane      boardWrapper;
    @FXML private GridPane  boardGrid;
    @FXML private Label     statusLabel;
    @FXML private HBox      dropButtonBar;

    @FXML private Button col0, col1, col2, col3, col4, col5, col6;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Button> colButtons;
    private Circle[][]   circles;        // [row][col] — rendered discs
    private Connect4Game game;
    private boolean      animating = false;   // block input during drop animation

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        colButtons = List.of(col0, col1, col2, col3, col4, col5, col6);
        game    = new Connect4Game();
        circles = new Circle[Connect4Game.ROWS][Connect4Game.COLS];

        buildBoard();
        wireColumnHover();
        updateStatus();
    }

    /** Called by MainMenuController to activate LAN mode before the scene is shown. */
    public void initLan() {
        isLan  = true;
        lobbyPane.setVisible(true);
        lobbyPane.setManaged(true);
        disableAllColumns();
        titleLabel.setText("4-Gewinnt – LAN");
        restartBtn.setText("Neustart anfragen");
        updateStatus();
    }

    // ── Lobby actions ─────────────────────────────────────────────────────────
    @FXML
    protected void onHostClick() {
        isHost  = true;
        myColor = Connect4Player.RED;
        hostBtn.setDisable(true);
        joinBtn.setDisable(true);
        server = new NetworkServer();
        server.setOnClientConnected(this::onPeerConnected);
        server.setOnMessage(this::handleMessage);
        server.setOnDisconnected(this::onPeerDisconnected);
        try {
            server.start(PORT);
            lobbyStatus.setText("Warte auf Mitspieler… (" + server.getLocalAddress() + ":" + PORT + ")");
        } catch (IOException e) {
            lobbyStatus.setText("Fehler: Port konnte nicht geöffnet werden.");
            e.printStackTrace();
        }
    }

    @FXML
    protected void onJoinClick() {
        isHost  = false;
        myColor = Connect4Player.YELLOW;
        hostBtn.setDisable(true);
        joinBtn.setDisable(true);
        ipBox.setVisible(true);
        ipBox.setManaged(true);
        lobbyStatus.setText("IP-Adresse des Hosts eingeben:");
    }

    @FXML
    protected void onConnectClick() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) { lobbyStatus.setText("Bitte IP-Adresse eingeben."); return; }
        connectBtn.setDisable(true);
        lobbyStatus.setText("Verbinde mit " + ip + "…");
        client = new NetworkClient();
        client.setOnConnected(this::onPeerConnected);
        client.setOnMessage(this::handleMessage);
        client.setOnDisconnected(this::onPeerDisconnected);
        client.connect(ip, PORT);
    }

    // ── Connection events ─────────────────────────────────────────────────────
    private void onPeerConnected() {
        if (isHost) server.send(new GameMessage(GameMessage.Type.PLAYER_ASSIGN, "YELLOW"));
        lobbyPane.setVisible(false);
        lobbyPane.setManaged(false);
        updateStatus();
    }

    private void onPeerDisconnected() {
        statusLabel.setText("Verbindung getrennt.");
        disableAllColumns();
    }

    // ── Incoming messages ─────────────────────────────────────────────────────
    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case PLAYER_ASSIGN -> {
                myColor = msg.getStringPayload().equals("RED")
                        ? Connect4Player.RED : Connect4Player.YELLOW;
                updateStatus();
            }
            case MOVE -> applyRemoteMove(msg.getIntPayload());
            case RESTART_REQ -> {
                if (restartPending) {
                    // Simultaneous request — treat as mutual agreement
                    doReset();
                    sendNet(new GameMessage(GameMessage.Type.RESTART_ACK));
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Dein Gegner möchte neu starten. Zustimmen?",
                            ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Neustart");
                    alert.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            sendNet(new GameMessage(GameMessage.Type.RESTART_ACK));
                            doReset();
                        }
                    });
                }
            }
            case RESTART_ACK -> { restartPending = false; doReset(); }
            case DISCONNECT   -> onPeerDisconnected();
        }
    }

    private void applyRemoteMove(int col) {
        int row = game.dropDisc(col);
        if (row == -1) return;
        Connect4Player player = game.getCell(row, col);
        Color discColor = (player == Connect4Player.RED) ? COLOR_RED : COLOR_YELLOW;
        animating = true;
        disableAllColumns();
        animateDrop(col, row, discColor, () -> {
            animating = false;
            if (!game.isGameOver()) enableMyColumns();
            updateStatus();
            if (game.isGameOver()) { animateWinLine(); dimLosingDiscs(); }
        });
        if (!game.isColumnPlayable(col)) colButtons.get(col).setDisable(true);
    }

    // ── Board construction ────────────────────────────────────────────────────
    private void buildBoard() {
        boardGrid.getChildren().clear();

        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                Circle disc = new Circle(DISC_RADIUS, COLOR_EMPTY);
                disc.setStroke(COLOR_EMPTY_RIM);
                disc.setStrokeWidth(2.5);
                circles[r][c] = disc;

                StackPane cell = new StackPane(disc);
                cell.setAlignment(Pos.CENTER);
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                boardGrid.add(cell, c, r);
            }
        }
    }

    /** Highlight the drop arrow when hovering a column. */
    private void wireColumnHover() {
        String normalStyle  = "-fx-font-size: 16px; -fx-background-color: transparent; " +
                "-fx-text-fill: #555577; -fx-cursor: hand; -fx-padding: 4 0;";
        String hoverStyle   = "-fx-font-size: 16px; -fx-background-color: transparent; " +
                "-fx-text-fill: #aaaadd; -fx-cursor: hand; -fx-padding: 4 0;";

        for (Button btn : colButtons) {
            btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) btn.setStyle(hoverStyle); });
            btn.setOnMouseExited (e -> btn.setStyle(normalStyle));
        }
    }

    // ── Column click ──────────────────────────────────────────────────────────
    @FXML
    protected void onColumnClick(javafx.event.ActionEvent event) {
        if (animating) return;
        // In LAN mode, reject clicks when it's not your turn
        if (isLan && game.getCurrentPlayer() != myColor) return;
        if (game.isGameOver()) return;

        Button clicked = (Button) event.getSource();
        int col = colButtons.indexOf(clicked);

        // Peek which row the disc will land in without committing yet
        int landingRow = peekLandingRow(col);
        if (landingRow == -1) return;

        // Commit the move in the model
        int row = game.dropDisc(col);
        if (row == -1) return;

        if (isLan) sendNet(new GameMessage(GameMessage.Type.MOVE, col));

        Connect4Player player = game.getCell(row, col);
        Color discColor = (player == Connect4Player.RED) ? COLOR_RED : COLOR_YELLOW;

        // Disable input during animation
        animating = true;
        disableAllColumns();

        animateDrop(col, row, discColor, () -> {
            animating = false;

            // Re-enable playable columns (unless game ended)
            if (!game.isGameOver()) {
                if (isLan) { /* opponent's turn — columns stay disabled */ }
                else {
                    for (int c = 0; c < Connect4Game.COLS; c++)
                        if (game.isColumnPlayable(c)) colButtons.get(c).setDisable(false);
                }
            }

            updateStatus();

            if (game.isGameOver()) {
                animateWinLine();
                dimLosingDiscs();
            }
        });

        // Disable this column if now full
        if (!game.isColumnPlayable(col)) clicked.setDisable(true);
    }

    /**
     * Returns which row a disc would land in for the given column,
     * without modifying game state.
     */
    private int peekLandingRow(int col) {
        for (int r = Connect4Game.ROWS - 1; r >= 0; r--) {
            if (game.getCell(r, col) == null) return r;
        }
        return -1;
    }

    /**
     * Animates a disc falling from the top of the column down to {@code targetRow}.
     * Calls {@code onDone} when the animation finishes.
     */
    private void animateDrop(int col, int targetRow, Color color, Runnable onDone) {
        Circle disc = circles[targetRow][col];

        // Temporarily show intermediate rows as the disc passes through them
        // by animating opacity of a "falling" overlay circle we create above the grid.
        // Simpler approach: animate the target disc's translateY from -(targetRow * cellSize)
        // down to 0, giving the illusion it dropped from the top.

        double dropDistance = (targetRow + 1) * CELL_SIZE;  // pixels to travel
        disc.setFill(color);
        disc.setStroke(color.brighter());
        disc.setStrokeWidth(2);
        disc.setTranslateY(-dropDistance);

        // Ease-in so it accelerates like gravity
        TranslateTransition tt = new TranslateTransition(Duration.millis(60 + targetRow * 38), disc);
        tt.setFromY(-dropDistance);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_IN);

        // Small bounce at the end
        tt.setOnFinished(e -> {
            ScaleTransition bounce = new ScaleTransition(Duration.millis(120), disc);
            bounce.setFromX(1.0); bounce.setFromY(1.0);
            bounce.setToX(1.12);  bounce.setToY(0.88);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(2);
            bounce.setOnFinished(e2 -> onDone.run());
            bounce.play();
        });

        tt.play();
    }

    // ── Win-line animation ────────────────────────────────────────────────────
    private void animateWinLine() {
        int[] cells = game.getWinningCells();
        if (cells == null) return;

        // cells contains 4 flat indices; start = first, end = last
        int flatStart = cells[0];
        int flatEnd   = cells[3];

        double[] s = discCentre(flatStart);
        double[] e = discCentre(flatEnd);

        final double OVERSHOOT = 32.0;
        double dx  = e[0] - s[0];
        double dy  = e[1] - s[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        double ux  = (len > 0) ? dx / len : 0;
        double uy  = (len > 0) ? dy / len : 0;

        double startX = s[0] - ux * OVERSHOOT;
        double startY = s[1] - uy * OVERSHOOT;
        double endX   = e[0] + ux * OVERSHOOT;
        double endY   = e[1] + uy * OVERSHOOT;

        Line winLine = new Line(startX, startY, startX, startY);
        winLine.setStroke(COLOR_WIN_LINE);
        winLine.setStrokeWidth(10);
        winLine.setStrokeLineCap(StrokeLineCap.ROUND);
        winLine.setOpacity(0.92);
        winLine.setMouseTransparent(true);

        boardWrapper.getChildren().add(winLine);

        KeyValue kvX = new KeyValue(winLine.endXProperty(), endX, Interpolator.EASE_OUT);
        KeyValue kvY = new KeyValue(winLine.endYProperty(), endY, Interpolator.EASE_OUT);
        new Timeline(new KeyFrame(Duration.millis(450), kvX, kvY)).play();
    }

    /**
     * Returns the centre of a disc (flat index) relative to boardWrapper's
     * top-left (i.e. including the board's own padding offset).
     */
    private double[] discCentre(int flat) {
        int row = flat / Connect4Game.COLS;
        int col = flat % Connect4Game.COLS;
        double x = BOARD_PAD + col * CELL_SIZE + CELL_SIZE / 2.0;
        double y = BOARD_PAD + row * CELL_SIZE + CELL_SIZE / 2.0;
        return new double[]{x, y};
    }

    private void dimLosingDiscs() {
        int[] winCells = game.getWinningCells();
        if (winCells == null) return;
        Set<Integer> winSet = new HashSet<>();
        for (int flat : winCells) winSet.add(flat);

        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                int flat = r * Connect4Game.COLS + c;
                Circle disc = circles[r][c];
                if (!winSet.contains(flat) && disc.getFill() != COLOR_EMPTY) {
                    FadeTransition ft = new FadeTransition(Duration.millis(350), disc);
                    ft.setToValue(0.25);
                    ft.play();
                } else if (winSet.contains(flat)) {
                    // Pulse winning discs
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(400), disc);
                    pulse.setFromX(1.0); pulse.setFromY(1.0);
                    pulse.setToX(1.10);  pulse.setToY(1.10);
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.play();
                }
            }
        }
    }

    // ── Restart / Back / Rules ────────────────────────────────────────────────
    @FXML
    protected void onRestartClick() {
        if (isLan) {
            restartPending = true;
            sendNet(new GameMessage(GameMessage.Type.RESTART_REQ));
            statusLabel.setText("Neustart angefragt…");
        } else {
            doReset();
        }
    }

    // Extracted from the old onRestartClick — called by both local restart and LAN ACK
    private void doReset() {
        restartPending = false;
        game.reset();
        animating = false;
        boardWrapper.getChildren().removeIf(n -> n instanceof Line);
        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                Circle disc = circles[r][c];
                disc.setFill(COLOR_EMPTY); disc.setStroke(COLOR_EMPTY_RIM);
                disc.setStrokeWidth(2.5);  disc.setOpacity(1.0);
                disc.setScaleX(1.0);       disc.setScaleY(1.0); disc.setTranslateY(0);
            }
        }
        if (isLan) enableMyColumns(); else colButtons.forEach(b -> b.setDisable(false));
        updateStatus();
    }

    private void enableMyColumns() {
        for (int c = 0; c < Connect4Game.COLS; c++)
            colButtons.get(c).setDisable(!game.isColumnPlayable(c));
    }

    private void sendNet(GameMessage msg) {
        if (isHost && server != null) server.send(msg);
        else if (!isHost && client != null) client.send(msg);
    }

    @FXML
    protected void onBackClick() {
        if (isLan) {
            sendNet(new GameMessage(GameMessage.Type.DISCONNECT));
            if (server != null) server.stop();
            if (client != null) client.stop();
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("main-menu.fxml"));
            boardGrid.getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    protected void onRulesClick() {
        showRulesDialog();
    }

    // ── Rules dialog ──────────────────────────────────────────────────────────
    private void showRulesDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("4-Gewinnt – Regeln");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DialogPane dp = dlg.getDialogPane();
        dp.setStyle("-fx-background-color: #13132a;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setPrefWidth(660);

        Label title = styledLabel("4-Gewinnt – So wird gespielt", "white", 22, true);

        Label rules = styledLabel(
                "• Zwei Spieler werfen abwechselnd Scheiben in ein 7×6 Gitter (Rot beginnt).\n" +
                        "• Die Scheibe fällt auf den untersten freien Platz der gewählten Spalte.\n" +
                        "• Wer zuerst vier eigene Scheiben in einer Reihe hat, gewinnt.\n" +
                        "• Vier in einer Reihe können waagerecht, senkrecht oder diagonal sein.\n" +
                        "• Ist das Gitter voll ohne Gewinner, endet das Spiel Unentschieden.",
                "#aaaacc", 14, false);
        rules.setWrapText(true);

        Label patTitle = styledLabel("Gewinnende Muster", "#ccccee", 16, true);

        VBox patterns = new VBox(20);
        patterns.setAlignment(Pos.CENTER_LEFT);
        patterns.getChildren().addAll(
                buildPatternCanvas("Waagerecht",  buildHorizontalPattern()),
                buildPatternCanvas("Senkrecht",   buildVerticalPattern()),
                buildPatternCanvas("Diagonal",  buildDiagPattern())
        );

        Label tipsTitle = styledLabel("Tipps", "#ccccee", 16, true);
        Label tips = styledLabel(
                "💡 Die mittleren Spalten (3, 4, 5) bieten die meisten Gewinnmöglichkeiten.\n" +
                        "💡 Versuche Gabeln zu bauen: zwei offene Dreierketten gleichzeitig.\n" +
                        "💡 Achte stets auf Bedrohungen deines Gegners und blocke sie frühzeitig.",
                "#8888bb", 13, false);
        tips.setWrapText(true);

        content.getChildren().addAll(title, rules, patTitle, patterns, tipsTitle, tips);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setStyle("-fx-background-color: #13132a; -fx-background: #13132a;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(540);

        dp.setContent(scroll);

        Button closeBtn = (Button) dp.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #2a2a4a; -fx-text-fill: #aaaacc; " +
                    "-fx-font-size: 14px; -fx-background-radius: 6; -fx-padding: 8 20; -fx-cursor: hand;");
        }

        dlg.showAndWait();
    }

    private Label styledLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;" +
                (bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }

    // ── Pattern canvas helpers ────────────────────────────────────────────────
    // Encoding: 0 = empty, 1 = RED (winner), 2 = YELLOW (other)

    /** Wraps a pattern canvas with a label below it. */
    private VBox buildPatternCanvas(String label, int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        double cs = 36;
        double w  = cols * cs + (cols - 1) * 2 + 2;
        double h  = rows * cs + (rows - 1) * 2 + 2;

        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();

        int[] first = null, last = null;

        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < cols; col++) {
                double x = 1 + col * (cs + 2);
                double y = 1 + r   * (cs + 2);

                gc.setFill(Color.web("#0a1628"));
                gc.fillRoundRect(x, y, cs, cs, 6, 6);

                int cell = grid[r][col];
                if (cell == 1 || cell == 2) {
                    Color base = (cell == 1) ? COLOR_RED : COLOR_YELLOW;
                    gc.setFill(base);
                    gc.fillOval(x + 4, y + 4, cs - 8, cs - 8);
                    gc.setStroke(base.brighter());
                    gc.setLineWidth(2);
                    gc.strokeOval(x + 4, y + 4, cs - 8, cs - 8);

                    if (cell == 1) {
                        double cx = x + cs / 2, cy = y + cs / 2;
                        if (first == null) first = new int[]{(int) cx, (int) cy};
                        last = new int[]{(int) cx, (int) cy};
                    }
                } else {
                    gc.setFill(Color.web("#0d1b2e"));
                    gc.fillOval(x + 4, y + 4, cs - 8, cs - 8);
                }
            }
        }

        // Win line through red cells
        if (first != null && last != null && !java.util.Arrays.equals(first, last)) {
            double dx2 = last[0] - first[0], dy2 = last[1] - first[1];
            double l2  = Math.sqrt(dx2 * dx2 + dy2 * dy2);
            double ux = l2 > 0 ? dx2 / l2 : 0, uy = l2 > 0 ? dy2 / l2 : 0;
            gc.setStroke(COLOR_WIN_LINE);
            gc.setLineWidth(3.5);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(first[0] - ux * 8, first[1] - uy * 8,
                    last[0]  + ux * 8,  last[1]  + uy * 8);
        }

        Label lbl = styledLabel(label, "#888899", 12, false);
        VBox box = new VBox(6, c, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private int[][] buildHorizontalPattern() {
        int[][] g = new int[3][6];
        g[2][1] = 1; g[2][2] = 1; g[2][3] = 1; g[2][4] = 1;
        g[2][0] = 2; g[2][5] = 2; g[1][1] = 2;
        return g;
    }

    private int[][] buildVerticalPattern() {
        int[][] g = new int[6][5];
        g[5][2] = 1; g[4][2] = 1; g[3][2] = 1; g[2][2] = 1;
        g[5][0] = 2; g[5][1] = 2; g[4][1] = 2;
        return g;
    }

    private int[][] buildDiagPattern() {
        int[][] g = new int[6][5];
        g[5][0] = 1; g[5][1] = 2; g[4][1] = 1; g[3][2] = 1; g[4][0] = 1; g[2][3] = 1;
        g[5][2] = 2; g[4][2] = 2; g[5][3] = 2; g[4][3] = 2; g[3][3] = 2; g[3][0] = 1;
        return g;
    }


    // ── Status ────────────────────────────────────────────────────────────────
    private void updateStatus() {
        switch (game.getGameState()) {
            case RED_WINS    -> setStatus("🔴  Rot gewinnt! 🎉",  "#e74c3c");
            case YELLOW_WINS -> setStatus("🟡  Gelb gewinnt! 🎉", "#f1c40f");
            case DRAW        -> setStatus("Unentschieden!",        "#f5c842");
            case PLAYING     -> {
                if (game.getCurrentPlayer() == Connect4Player.RED)
                    setStatus("🔴  Rot ist dran", "#e74c3c");
                else
                    setStatus("🟡  Gelb ist dran", "#f1c40f");
            }
        }
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color +
                "; -fx-font-size: 26px; -fx-font-weight: bold;");
    }

    private void disableAllColumns() {
        colButtons.forEach(b -> b.setDisable(true));
    }
}