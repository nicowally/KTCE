package gamehub.gamehub;

import gamehub.games.ticTacToe.*;
import gamehub.network.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Modern Tic-Tac-Toe controller.
 *
 * Rendering approach
 * ──────────────────
 * Each board cell is a StackPane containing:
 *   • A background Canvas (draws the cell background & thick grid borders)
 *   • A symbol Canvas (draws the animated X or O on top)
 *   • An invisible overlay Region that captures mouse clicks
 *
 * Symbols are drawn on a Canvas with a stroke-dash animation so they
 * "write themselves in" (X = two diagonals, O = arc growing 0→360°).
 *
 * The win line is an animated javafx.scene.shape.Line laid over the grid.
 */
public class TicTacToeController {

    // ── LAN state ─────────────────────────────────────────────────────────────
    private boolean       isLan          = false;
    private boolean       isHost         = false;
    private boolean       restartPending = false;
    private Player        myPlayer       = null;   // assigned after lobby
    private NetworkServer server         = null;
    private NetworkClient client         = null;

    private static final int PORT = 55502;   // distinct from Connect4's 55501

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_CELL        = Color.web("#13132a");
    private static final Color BG_CELL_HOVER  = Color.web("#1c1c38");
    private static final Color BG_CELL_WIN    = Color.web("#1a2a1a");
    private static final Color COLOR_GRID     = Color.web("#2e2e55");
    private static final Color COLOR_X        = Color.web("#4af0c8");
    private static final Color COLOR_O        = Color.web("#f05a7e");
    private static final Color COLOR_WIN_LINE = Color.web("#f5c842");
    private static final Color COLOR_DIM      = Color.web("#888888", 0.35);

    private static final double SYMBOL_STROKE = 8.0;
    private static final double GRID_STROKE   = 4.0;
    private static final double CELL_SIZE     = 140.0;

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Pane      boardWrapper;   // absolute-positioned container for win-line
    @FXML private GridPane  boardGrid;
    @FXML private Label     statusLabel;
    @FXML private HBox      sideSelectionBox;
    @FXML private Button    playerSideBtn;
    @FXML private Button    aiSideBtn;
    @FXML private VBox      difficultyBox;
    @FXML private Slider    difficultySlider;
    @FXML private Label     difficultyLabel;

    // ── LAN lobby FXML ────────────────────────────────────────────────────────
    @FXML private VBox      lobbyPane;
    @FXML private Label     lobbyStatus;
    @FXML private Button    hostBtn;
    @FXML private Button    joinBtn;
    @FXML private HBox      ipBox;
    @FXML private TextField ipField;
    @FXML private Button    connectBtn;

    // ── Per-cell state ────────────────────────────────────────────────────────
    private final Canvas[]    symbolCanvases = new Canvas[9];
    private final Region[]    overlays       = new Region[9];
    private final StackPane[] cellPanes      = new StackPane[9];
    private final Timeline[]  drawAnims      = new Timeline[9];

    private TicTacToeGame game;
    private boolean vsAI        = false;
    private Player  humanPlayer = Player.X;
    private int     difficulty  = 100;
    private boolean gameStarted = false;

    // ── Public API ────────────────────────────────────────────────────────────
    public void initMode(boolean vsAI) { this.vsAI = vsAI; }

    /**
     * Called by MainMenuController to activate LAN mode before the scene is shown.
     * Mirrors Connect4Controller.initLan().
     */
    public void initLan() {
        isLan = true;
        lobbyPane.setVisible(true);
        lobbyPane.setManaged(true);
        freezeBoard();
        statusLabel.setText("Warte auf LAN-Verbindung…");
        statusLabel.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 22px; -fx-font-weight: bold;");
    }

    // ── FXML lifecycle ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        game = new TicTacToeGame();
        buildBoard();
    }

    public void postInit() {
        if (vsAI) {
            sideSelectionBox.setVisible(true);  sideSelectionBox.setManaged(true);
            difficultyBox.setVisible(true);     difficultyBox.setManaged(true);
        } else {
            sideSelectionBox.setVisible(false); sideSelectionBox.setManaged(false);
            difficultyBox.setVisible(false);    difficultyBox.setManaged(false);
        }

        difficultySlider.setMin(0);
        difficultySlider.setMax(100);
        difficultySlider.setValue(difficulty);
        difficultySlider.valueProperty().addListener((obs, o, n) -> {
            difficulty = n.intValue();
            difficultyLabel.setText(TicTacToeAI.difficultyLabel(difficulty));
        });
        difficultyLabel.setText(TicTacToeAI.difficultyLabel(difficulty));

        refreshSideButtons();
        updateStatus();

        if (vsAI && humanPlayer == Player.O) scheduleAiMove(900);
    }

    // ── Lobby actions ─────────────────────────────────────────────────────────
    @FXML
    protected void onHostClick() {
        isHost   = true;
        myPlayer = Player.X;   // host is always X (X goes first)
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
        isHost   = false;
        myPlayer = Player.O;   // client is always O
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
        // Host tells client which symbol it has been assigned
        if (isHost) server.send(new GameMessage(GameMessage.Type.PLAYER_ASSIGN, "O"));
        lobbyPane.setVisible(false);
        lobbyPane.setManaged(false);
        updateStatus();
        // X (host) goes first — enable board for host only
        if (myPlayer == Player.X) unfreezeBoard();
    }

    private void onPeerDisconnected() {
        statusLabel.setText("Verbindung getrennt.");
        statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 22px; -fx-font-weight: bold;");
        freezeBoard();
    }

    // ── Incoming messages ─────────────────────────────────────────────────────
    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case PLAYER_ASSIGN -> {
                myPlayer = msg.getStringPayload().equals("X") ? Player.X : Player.O;
                updateStatus();
                // X moves first; if we're O, board stays frozen until host moves
            }
            case MOVE -> applyRemoteMove(msg.getIntPayload());
            case RESTART_REQ -> {
                if (restartPending) {
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
            case DISCONNECT  -> onPeerDisconnected();
        }
    }

    private void applyRemoteMove(int idx) {
        if (!game.makeMove(idx)) return;
        animateSymbol(idx, game.getCell(idx), false);
        updateStatus();
        if (game.isGameOver()) {
            scheduleWinEffects();
            freezeBoard();
        } else {
            // Now it's our turn
            unfreezeBoard();
        }
    }

    // ── Board construction ────────────────────────────────────────────────────
    private void buildBoard() {
        boardGrid.getChildren().clear();

        for (int i = 0; i < 9; i++) {
            final int idx = i;
            int row = i / 3;
            int col = i % 3;

            // Background canvas — draws cell bg + grid borders
            Canvas bgCanvas = new Canvas(CELL_SIZE, CELL_SIZE);
            drawCellBackground(bgCanvas, row, col, false, false);

            // Symbol canvas — draws animated X / O
            Canvas symCanvas = new Canvas(CELL_SIZE, CELL_SIZE);
            symbolCanvases[i] = symCanvas;

            // Invisible overlay for clicks
            Region overlay = new Region();
            overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            overlay.setStyle("-fx-cursor: hand;");
            overlay.setOnMouseClicked(e -> onCellClicked(idx));
            overlay.setOnMouseEntered(e -> {
                if (isCellClickable(idx)) drawCellBackground(bgCanvas, row, col, true, false);
            });
            overlay.setOnMouseExited(e -> drawCellBackground(bgCanvas, row, col, false, false));
            overlays[i] = overlay;

            StackPane pane = new StackPane(bgCanvas, symCanvas, overlay);
            pane.setPrefSize(CELL_SIZE, CELL_SIZE);
            cellPanes[i] = pane;

            GridPane.setRowIndex(pane, row);
            GridPane.setColumnIndex(pane, col);
            boardGrid.getChildren().add(pane);
        }
    }

    /** Draws the cell background + appropriate grid borders onto bgCanvas. */
    private void drawCellBackground(Canvas canvas, int row, int col,
                                    boolean hover, boolean win) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(win ? BG_CELL_WIN : (hover ? BG_CELL_HOVER : BG_CELL));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(GRID_STROKE);
        if (col < 2) gc.strokeLine(w - GRID_STROKE / 2, 0, w - GRID_STROKE / 2, h);
        if (row < 2) gc.strokeLine(0, h - GRID_STROKE / 2, w, h - GRID_STROKE / 2);
    }

    // ── Cell click ────────────────────────────────────────────────────────────
    private boolean isCellClickable(int idx) {
        if (game.getCell(idx) != null || game.isGameOver()) return false;
        if (isLan)  return myPlayer != null && game.getCurrentPlayer() == myPlayer;
        if (vsAI)   return game.getCurrentPlayer() == humanPlayer;
        return true;
    }

    private void onCellClicked(int idx) {
        if (!isCellClickable(idx)) return;
        if (!game.makeMove(idx)) return;
        lockDifficultyIfFirstMove();
        animateSymbol(idx, game.getCell(idx), false);
        updateStatus();

        if (isLan) {
            sendNet(new GameMessage(GameMessage.Type.MOVE, idx));
        }
        if (game.isGameOver()) {
            scheduleWinEffects();
            freezeBoard();
            return;
        } if (isLan){
            freezeBoard();   // opponent's turn
        } else if (vsAI) {
            scheduleAiMove(420);
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────

    /**
     * Animates drawing an X or O symbol at cell {@code idx}.
     *
     * @param dim  if true the symbol will be drawn dimmed (losing cells)
     */
    private void animateSymbol(int idx, Player player, boolean dim) {
        if (drawAnims[idx] != null) drawAnims[idx].stop();

        Canvas canvas = symbolCanvases[idx];
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double pad = 28.0;
        Color baseColor = dim ? COLOR_DIM : (player == Player.X ? COLOR_X : COLOR_O);

        Timeline tl = new Timeline();
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(280), e -> {}));
        AnimationTimer timer = new AnimationTimer() {
            long startNs = -1;
            final double durationMs = 270;

            @Override
            public void handle(long now) {
                if (startNs < 0) startNs = now;
                double p = Math.min(1.0, (now - startNs) / 1_000_000.0 / durationMs);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, w, h);
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.setLineWidth(SYMBOL_STROKE);
                gc.setStroke(baseColor);
                if (player == Player.X) drawXProgress(gc, pad, w, h, p);
                else                    drawOProgress(gc, pad, w, h, p);
                if (p >= 1.0) stop();
            }
        };
        drawAnims[idx] = tl;
        timer.start();

        // Marks overlay as non-interactive
        overlays[idx].setStyle("-fx-cursor: default;");
        overlays[idx].setOnMouseEntered(null);
        overlays[idx].setOnMouseExited(null);
    }

    /** Draw an X stroked up to {@code t} ∈ [0,1]. Two diagonals each half the time. */
    private void drawXProgress(GraphicsContext gc, double pad, double w, double h, double t) {
        double x0 = pad, y0 = pad, x1 = w - pad, y1 = h - pad;
        double t1 = Math.min(1.0, t * 2);
        gc.strokeLine(x0, y0, x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1);
        if (t > 0.5) {
            double t2 = (t - 0.5) * 2;
            gc.strokeLine(x1, y0, x1 - (x1 - x0) * t2, y0 + (y1 - y0) * t2);
        }
    }

    /** Draw an O (arc) stroked up to {@code t} ∈ [0,1]. */
    private void drawOProgress(GraphicsContext gc, double pad, double w, double h, double t) {
        double cx = w / 2, cy = h / 2;
        double r  = (Math.min(w, h) / 2) - pad;
        int segments = 90;
        double sweep = 360.0 * t;
        double startAngle = -90;
        gc.beginPath();
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(startAngle + sweep * i / segments);
            double px = cx + r * Math.cos(ang);
            double py = cy + r * Math.sin(ang);
            if (i == 0) gc.moveTo(px, py);
            else        gc.lineTo(px, py);
        }
        gc.stroke();
    }

    /** Animate the winning line across the board. */
    private void animateWinLine(int[] line) {
        final double OVERSHOOT = 46.0;

        double[] s = cellCentre(line[0]);
        double[] e = cellCentre(line[2]);

        // Direction unit vector (for overshoot extension)
        double dx = e[0] - s[0];
        double dy = e[1] - s[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        double ux = (len > 0) ? dx / len : 0;
        double uy = (len > 0) ? dy / len : 0;

        double startX = s[0] - ux * OVERSHOOT;
        double startY = s[1] - uy * OVERSHOOT;
        double endX   = e[0] + ux * OVERSHOOT;
        double endY   = e[1] + uy * OVERSHOOT;

        Line winLine = new Line(startX, startY, endX, endY);
        winLine.setStroke(COLOR_WIN_LINE);
        winLine.setStrokeWidth(10);
        winLine.setStrokeLineCap(StrokeLineCap.ROUND);
        winLine.setOpacity(0.92);
        winLine.setMouseTransparent(true);
        boardWrapper.getChildren().add(winLine);

        KeyValue kvX = new KeyValue(winLine.endXProperty(), endX, Interpolator.EASE_OUT);
        KeyValue kvY = new KeyValue(winLine.endYProperty(), endY, Interpolator.EASE_OUT);
        Timeline tl  = new Timeline(new KeyFrame(Duration.millis(420), kvX, kvY));
        tl.play();

        // Dims non-winning cells
        for (int i = 0; i < 9; i++) {
            boolean inLine = false;
            for (int li : line) if (li == i) inLine = true;
            if (!inLine && game.getCell(i) != null) dimCell(i);
        }
    }

    /** Returns the centre point (x,y) of cell idx relative to the boardGrid. */
    private double[] cellCentre(int idx) {
        int col = idx % 3;
        int row = idx / 3;
        return new double[]{col * CELL_SIZE + CELL_SIZE / 2,
                row * CELL_SIZE + CELL_SIZE / 2};
    }

    /** Fade a cell's symbol to dim. */
    private void dimCell(int idx) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), symbolCanvases[idx]);
        ft.setFromValue(1.0);
        ft.setToValue(0.28);
        ft.play();
    }

    // ── Side-selection (local/AI only) ────────────────────────────────────────
    @FXML
    protected void onPlayerSideBtnClick() {
        humanPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
        refreshSideButtons();
        restartGame();
    }

    @FXML
    protected void onAiSideBtnClick() {
        humanPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
        refreshSideButtons();
        restartGame();
    }

    private void refreshSideButtons() {
        String pSym = (humanPlayer == Player.X) ? "X" : "O";
        String aSym = (humanPlayer == Player.X) ? "O" : "X";
        playerSideBtn.setText("Spieler  " + pSym);
        aiSideBtn.setText("KI  " + aSym);

        String active   = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #4a90d9; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 9 22;";
        String inactive = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #1e1e3a; -fx-text-fill: #8888bb; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 9 22;";

        playerSideBtn.setStyle(humanPlayer == Player.X ? active : inactive);
        aiSideBtn.setStyle(humanPlayer == Player.X ? inactive : active);
    }

    // ── Restart / Back / Rules ────────────────────────────────────────────────
    @FXML
    protected void onRestartClick() {
        if (isLan) {
            restartPending = true;
            sendNet(new GameMessage(GameMessage.Type.RESTART_REQ));
            statusLabel.setText("Neustart angefragt…");
            statusLabel.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 22px; -fx-font-weight: bold;");
        } else {
            restartGame();
        }
    }

    private void doReset() {
        restartPending = false;
        restartGame();
        // In LAN: X (host) always goes first after reset
        if (isLan) {
            if (myPlayer == Player.X) unfreezeBoard(); else freezeBoard();
        }
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void sendNet(GameMessage msg) {
        if (isHost && server != null) server.send(msg);
        else if (!isHost && client != null) client.send(msg);
    }

    private void freezeBoard() {
        for (Region ov : overlays) { if (ov != null) ov.setMouseTransparent(true); }
    }

    /**
     * Re-enables only empty cells (preserves the pointer-transparent style on
     * already-played cells set inside animateSymbol).
     */
    private void unfreezeBoard() {
        for (int i = 0; i < 9; i++) {
            if (overlays[i] != null && game.getCell(i) == null && !game.isGameOver()) {
                overlays[i].setMouseTransparent(false);
                final int idx = i;
                final int row = i / 3, col = i % 3;
                Canvas bg = (Canvas) cellPanes[i].getChildren().get(0);
                overlays[i].setStyle("-fx-cursor: hand;");
                overlays[i].setOnMouseEntered(e -> {
                    if (isCellClickable(idx)) drawCellBackground(bg, row, col, true, false);
                });
                overlays[i].setOnMouseExited(e -> drawCellBackground(bg, row, col, false, false));
            }
        }
    }

    private void lockDifficultyIfFirstMove() {
        if (!gameStarted) {
            gameStarted = true;
            if (difficultySlider != null) difficultySlider.setDisable(true);
        }
    }

    private void scheduleAiMove(long delayMs) {
        for (Region ov : overlays) { if (ov != null) ov.setMouseTransparent(true); }
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> Platform.runLater(() -> {
            if (game.isGameOver()) return;
            for (int i = 0; i < 9; i++)
                if (overlays[i] != null) overlays[i].setMouseTransparent(false);

            Player aiPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
            int move = TicTacToeAI.getBestMove(game.getBoard(), aiPlayer, difficulty);
            if (move == -1) return;
            game.makeMove(move);
            lockDifficultyIfFirstMove();
            animateSymbol(move, game.getCell(move), false);
            updateStatus();
            if (game.isGameOver()) freezeBoard();
        }));
        pause.play();
    }

    // ── Status ────────────────────────────────────────────────────────────────
    private void updateStatus() {
        switch (game.getGameState()) {
            case X_WINS -> {
                String w = resolveWinner(Player.X);
                setStatus(w + " gewinnt! 🎉", "#4af0c8");
                scheduleWinEffects();
                freezeBoard();
            }
            case O_WINS -> {
                String w = resolveWinner(Player.O);
                setStatus(w + " gewinnt! 🎉", "#f05a7e");
                scheduleWinEffects();
                freezeBoard();
            }
            case DRAW -> {
                setStatus("Unentschieden!", "#f5c842");
                freezeBoard();
            }
            case PLAYING -> {
                Player cur = game.getCurrentPlayer();
                String txt;
                if (isLan && myPlayer != null) {
                    boolean myTurn = cur == myPlayer;
                    txt = myTurn ? "Du bist dran (" + cur + ")" : "Gegner ist dran (" + cur + ")";
                } else if (vsAI) {
                    txt = (cur == humanPlayer) ? "Du bist dran (" + cur + ")" : "KI denkt… (" + cur + ")";
                } else {
                    txt = "Spieler " + cur + " ist an der Reihe";
                }
                Color c = (cur == Player.X) ? COLOR_X : COLOR_O;
                setStatus(txt, toHex(c));
            }
        }
    }

    private void scheduleWinEffects() {
        PauseTransition pt = new PauseTransition(Duration.millis(320));
        pt.setOnFinished(e -> {
            int[] wl = game.getWinningLine();
            if (wl != null) animateWinLine(wl);
        });
        pt.play();
    }

    private void setStatus(String text, String hexColor) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 26px; -fx-font-weight: bold;");
    }

    private String resolveWinner(Player w) {
        if (isLan)  return (w == myPlayer) ? "Du" : "Gegner";
        if (!vsAI)  return "Spieler " + w;
        return (w == humanPlayer) ? "Du" : "KI";
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }

    private void restartGame() {
        for (int i = 0; i < 9; i++)
            if (drawAnims[i] != null) drawAnims[i].stop();
        game.reset();
        gameStarted = false;
        boardWrapper.getChildren().removeIf(n -> n instanceof Line);
        buildBoard();
        if (difficultySlider != null) difficultySlider.setDisable(false);
        updateStatus();
        if (!isLan && vsAI && humanPlayer == Player.O) scheduleAiMove(900);
    }

    // ── Rules dialog ──────────────────────────────────────────────────────────
    private void showRulesDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Tic-Tac-Toe – Regeln");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        DialogPane dp = dlg.getDialogPane();
        dp.setStyle("-fx-background-color: #13132a;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setPrefWidth(640);

        Label title = new Label("So funktioniert Tic-Tac-Toe");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label rules = new Label(
                "• Zwei Spieler setzen abwechselnd X und O auf einem 3×3-Feld.\n" +
                        "• Ziel: Drei eigene Symbole in einer Reihe (waagerecht, senkrecht oder diagonal).\n" +
                        "• Wer zuerst drei in einer Reihe hat, gewinnt.\n" +
                        "• Wenn das Brett voll ist und niemand gewonnen hat, endet das Spiel Unentschieden.\n" +
                        "• X beginnt immer als erster Spieler."
        );
        rules.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 14px; -fx-wrap-text: true;");
        rules.setWrapText(true);

        Label outcomeTitle = new Label("Gewinnende Muster");
        outcomeTitle.setStyle("-fx-text-fill: #ccccee; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox outcomes = new HBox(16);
        outcomes.setAlignment(Pos.CENTER_LEFT);
        outcomes.getChildren().addAll(
                buildOutcomeBoard("Zeile",
                        new String[]{"X","X","X","O","·","O","·","·","·"},
                        new int[]{0,1,2}),
                buildOutcomeBoard("Spalte",
                        new String[]{"X","O","·","X","O","·","X","·","·"},
                        new int[]{0,3,6}),
                buildOutcomeBoard("Diagonale",
                        new String[]{"X","O","·","O","X","·","·","·","X"},
                        new int[]{0,4,8}),
                buildOutcomeBoard("Unentschieden",
                        new String[]{"X","O","X","X","X","O","O","X","O"},
                        new int[]{})
        );

        Label tipsTitle = new Label("Tipps");
        tipsTitle.setStyle("-fx-text-fill: #ccccee; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label tips = new Label(
                "💡 Die Mitte (Feld 5) ist die stärkste Position – belege sie zuerst!\n" +
                        "💡 Ecken sind stärker als Randfelder.\n" +
                        "💡 Achte darauf, die Gewinnchancen deines Gegners zu blockieren."
        );
        tips.setStyle("-fx-text-fill: #8888bb; -fx-font-size: 13px; -fx-wrap-text: true;");
        tips.setWrapText(true);

        content.getChildren().addAll(title, rules, outcomeTitle, outcomes, tipsTitle, tips);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setStyle("-fx-background-color: #13132a; -fx-background: #13132a;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(520);
        dp.setContent(scroll);

        Button closeBtn = (Button) dp.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #2a2a4a; -fx-text-fill: #aaaacc; -fx-font-size: 14px; " +
                    "-fx-background-radius: 6; -fx-padding: 8 20; -fx-cursor: hand;");
        }

        dlg.showAndWait();
    }

    /**
     * Builds a mini 3×3 board canvas for the rules dialog.
     *
     * @param label   title below the board
     * @param symbols 9 entries: "X", "O", or "·" for empty
     * @param winLine indices of the 3 winning cells (empty for draw/no-win)
     */
    private VBox buildOutcomeBoard(String label, String[] symbols, int[] winLine) {
        double cs = 44;
        double total = cs * 3 + 6;
        Canvas c = new Canvas(total, total);
        GraphicsContext gc = c.getGraphicsContext2D();

        java.util.Set<Integer> winSet = new java.util.HashSet<>();
        for (int w : winLine) winSet.add(w);

        gc.setFill(Color.web("#0d0d1a"));
        gc.fillRect(0, 0, total, total);

        for (int i = 0; i < 9; i++) {
            int r = i / 3;
            int col = i % 3;
            double x = col * (cs + 3);
            double y = r  * (cs + 3);

            boolean isWin = winSet.contains(i);
            gc.setFill(isWin ? Color.web("#1a2a1a") : Color.web("#13132a"));
            gc.fillRoundRect(x, y, cs, cs, 4, 4);

            String sym = symbols[i];
            if (!sym.equals("·")) {
                Color clr = sym.equals("X") ? COLOR_X : COLOR_O;
                gc.setStroke(isWin ? clr.brighter() : (winLine.length > 0 ? clr.deriveColor(0,1,1,0.35) : clr));
                gc.setLineWidth(3.0);
                gc.setLineCap(StrokeLineCap.ROUND);
                double pad = 8;
                if (sym.equals("X")) {
                    gc.strokeLine(x + pad, y + pad, x + cs - pad, y + cs - pad);
                    gc.strokeLine(x + cs - pad, y + pad, x + pad, y + cs - pad);
                } else {
                    double cx2 = x + cs / 2, cy2 = y + cs / 2;
                    double rad = cs / 2 - pad;
                    gc.strokeOval(cx2 - rad, cy2 - rad, rad * 2, rad * 2);
                }
            }

            // Grid borders
            gc.setStroke(Color.web("#2e2e55"));
            gc.setLineWidth(1.5);
            if (col < 2) gc.strokeLine(x + cs + 1, y, x + cs + 1, y + cs);
            if (r   < 2) gc.strokeLine(x, y + cs + 1, x + cs, y + cs + 1);
        }

        if (winLine.length == 3) {
            double[] s = miniCentre(winLine[0], cs);
            double[] e = miniCentre(winLine[2], cs);
            gc.setStroke(COLOR_WIN_LINE);
            gc.setLineWidth(3.5);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(s[0], s[1], e[0], e[1]);
        }

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #888899; -fx-font-size: 12px;");
        VBox box = new VBox(6, c, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private double[] miniCentre(int idx, double cs) {
        return new double[]{idx%3*(cs+3)+cs/2, idx/3*(cs+3)+cs/2};
    }
}