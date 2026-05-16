package gamehub.gamehub;

import gamehub.games.ticTacToe.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_CELL        = Color.web("#13132a");
    private static final Color BG_CELL_HOVER  = Color.web("#1c1c38");
    private static final Color BG_CELL_WIN    = Color.web("#1a2a1a");
    private static final Color COLOR_GRID     = Color.web("#2e2e55");
    private static final Color COLOR_X        = Color.web("#4af0c8");   // teal-neon
    private static final Color COLOR_O        = Color.web("#f05a7e");   // coral-neon
    private static final Color COLOR_WIN_LINE = Color.web("#f5c842");   // gold
    private static final Color COLOR_DIM      = Color.web("#888888", 0.35);

    private static final double SYMBOL_STROKE = 8.0;
    private static final double GRID_STROKE   = 4.0;
    private static final double CELL_SIZE     = 140.0;   // matches FXML prefWidth/Height

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private GridPane boardGrid;
    @FXML private Label    statusLabel;
    @FXML private HBox     sideSelectionBox;
    @FXML private Button   playerSideBtn;
    @FXML private Button   aiSideBtn;
    @FXML private VBox     difficultyBox;
    @FXML private Slider   difficultySlider;
    @FXML private Label    difficultyLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    /** Per-cell symbol canvases (index 0-8). */
    private final Canvas[] symbolCanvases = new Canvas[9];
    /** Per-cell overlay regions for click detection. */
    private final Region[] overlays       = new Region[9];
    /** Per-cell StackPanes. */
    private final StackPane[] cellPanes   = new StackPane[9];
    /** Running draw animations (so we can cancel on restart). */
    private final Timeline[] drawAnims    = new Timeline[9];

    private TicTacToeGame game;
    private boolean vsAI        = false;
    private Player  humanPlayer = Player.X;
    private int     difficulty  = 100;
    private boolean gameStarted = false;

    // ── Public API ────────────────────────────────────────────────────────────
    public void initMode(boolean vsAI) { this.vsAI = vsAI; }

    // ── FXML lifecycle ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        game = new TicTacToeGame();
        buildBoard();
    }

    public void postInit() {
        // AI-only widgets
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
            overlay.setOnMouseExited(e -> {
                drawCellBackground(bgCanvas, row, col, false, false);
            });
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

        // Cell fill
        gc.setFill(win ? BG_CELL_WIN : (hover ? BG_CELL_HOVER : BG_CELL));
        gc.fillRect(0, 0, w, h);

        // Thick grid lines: right border for col 0-1, bottom border for row 0-1
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(GRID_STROKE);
        if (col < 2) { // right border
            gc.strokeLine(w - GRID_STROKE / 2, 0, w - GRID_STROKE / 2, h);
        }
        if (row < 2) { // bottom border
            gc.strokeLine(0, h - GRID_STROKE / 2, w, h - GRID_STROKE / 2);
        }
    }

    // ── Cell click ────────────────────────────────────────────────────────────
    private boolean isCellClickable(int idx) {
        return game.getCell(idx) == null
                && !game.isGameOver()
                && !(vsAI && game.getCurrentPlayer() != humanPlayer);
    }

    private void onCellClicked(int idx) {
        if (!isCellClickable(idx)) return;
        if (!game.makeMove(idx)) return;
        lockDifficultyIfFirstMove();
        animateSymbol(idx, game.getCell(idx), false);
        updateStatus();
        if (!game.isGameOver() && vsAI) scheduleAiMove(420);
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

        // progress 0→1 drives the draw
        final double[] progress = {0.0};

        Timeline tl = new Timeline();
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(280), e -> {}));
        // We animate manually via a per-frame callback
        AnimationTimer timer = new AnimationTimer() {
            long startNs = -1;
            final double durationMs = 270;

            @Override
            public void handle(long now) {
                if (startNs < 0) startNs = now;
                double elapsed = (now - startNs) / 1_000_000.0;
                progress[0] = Math.min(1.0, elapsed / durationMs);

                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, w, h);
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.setLineWidth(SYMBOL_STROKE);
                gc.setStroke(baseColor);

                if (player == Player.X) {
                    drawXProgress(gc, pad, w, h, progress[0]);
                } else {
                    drawOProgress(gc, pad, w, h, progress[0]);
                }

                if (progress[0] >= 1.0) stop();
            }
        };
        drawAnims[idx] = tl;   // just track it so we can cancel
        timer.start();
        // Mark overlay as non-interactive
        overlays[idx].setStyle("-fx-cursor: default;");
        overlays[idx].setOnMouseEntered(null);
        overlays[idx].setOnMouseExited(null);
    }

    /** Draw an X stroked up to {@code t} ∈ [0,1]. Two diagonals each half the time. */
    private void drawXProgress(GraphicsContext gc, double pad, double w, double h, double t) {
        double x0 = pad, y0 = pad, x1 = w - pad, y1 = h - pad;
        // First diagonal: top-left → bottom-right
        double t1 = Math.min(1.0, t * 2);
        gc.strokeLine(x0, y0, x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1);
        // Second diagonal: top-right → bottom-left
        if (t > 0.5) {
            double t2 = (t - 0.5) * 2;
            gc.strokeLine(x1, y0, x1 - (x1 - x0) * t2, y0 + (y1 - y0) * t2);
        }
    }

    /** Draw an O (arc) stroked up to {@code t} ∈ [0,1]. */
    private void drawOProgress(GraphicsContext gc, double pad, double w, double h, double t) {
        double cx = w / 2, cy = h / 2;
        double r  = (Math.min(w, h) / 2) - pad;
        // JavaFX strokeArc draws the arc of a rectangle; use a temp approach with
        // path segments for smooth stroke animation
        int segments = 90;
        double sweep = 360.0 * t;
        double startAngle = -90; // start at top

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
        // Compute start and end centres of the winning cells
        double[] start = cellCentre(line[0]);
        double[] end   = cellCentre(line[2]);

        Line winLine = new Line(start[0], start[1], start[0], start[1]);
        winLine.setStroke(COLOR_WIN_LINE);
        winLine.setStrokeWidth(6);
        winLine.setStrokeLineCap(StrokeLineCap.ROUND);
        winLine.setOpacity(0.9);

        // Add the line on top of the boardGrid inside its parent StackPane
        // We wrap boardGrid in a StackPane overlay approach using a Pane sibling
        StackPane boardWrapper = getBoardWrapper();
        if (boardWrapper != null) boardWrapper.getChildren().add(winLine);

        KeyValue kvX = new KeyValue(winLine.endXProperty(), end[0], Interpolator.EASE_OUT);
        KeyValue kvY = new KeyValue(winLine.endYProperty(), end[1], Interpolator.EASE_OUT);
        Timeline tl  = new Timeline(new KeyFrame(Duration.millis(380), kvX, kvY));
        tl.play();

        // Dim non-winning cells
        for (int i = 0; i < 9; i++) {
            boolean inLine = false;
            for (int li : line) if (li == i) inLine = true;
            if (!inLine && game.getCell(i) != null) {
                dimCell(i);
            }
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
        Canvas c = symbolCanvases[idx];
        FadeTransition ft = new FadeTransition(Duration.millis(300), c);
        ft.setFromValue(1.0);
        ft.setToValue(0.28);
        ft.play();
    }

    /** Finds the StackPane that wraps the boardGrid (for win-line overlay). */
    private StackPane getBoardWrapper() {
        Node parent = boardGrid.getParent();
        while (parent != null) {
            if (parent instanceof StackPane sp) return sp;
            parent = parent.getParent();
        }
        return null;
    }

    // ── Side-selection ────────────────────────────────────────────────────────
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

        if (humanPlayer == Player.X) {
            playerSideBtn.setStyle(active);
            aiSideBtn.setStyle(inactive);
        } else {
            playerSideBtn.setStyle(inactive);
            aiSideBtn.setStyle(active);
        }
    }

    // ── Restart / Back / Rules ────────────────────────────────────────────────
    @FXML
    protected void onRestartClick() { restartGame(); }

    @FXML
    protected void onBackClick() {
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
        dlg.setTitle("Tic-Tac-Toe – Regeln");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Style the dialog pane
        DialogPane dp = dlg.getDialogPane();
        dp.setStyle("-fx-background-color: #13132a;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setPrefWidth(640);

        // Title
        Label title = new Label("So funktioniert Tic-Tac-Toe");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        // Rules text
        Label rules = new Label(
                "• Zwei Spieler setzen abwechselnd X und O auf einem 3×3-Feld.\n" +
                        "• Ziel: Drei eigene Symbole in einer Reihe (waagerecht, senkrecht oder diagonal).\n" +
                        "• Wer zuerst drei in einer Reihe hat, gewinnt.\n" +
                        "• Wenn das Brett voll ist und niemand gewonnen hat, endet das Spiel Unentschieden.\n" +
                        "• X beginnt immer als erster Spieler."
        );
        rules.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 14px; -fx-wrap-text: true;");
        rules.setWrapText(true);

        // Outcome illustrations
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

        // Tips
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
        dp.lookup(".close-button-bar") ;
        // Style the close button
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
        double cs = 44;   // mini cell size
        double total = cs * 3 + 6; // 3 cells + 2 gaps of 3px
        Canvas c = new Canvas(total, total);
        GraphicsContext gc = c.getGraphicsContext2D();

        java.util.Set<Integer> winSet = new java.util.HashSet<>();
        for (int w : winLine) winSet.add(w);

        // Grid background
        gc.setFill(Color.web("#0d0d1a"));
        gc.fillRect(0, 0, total, total);

        for (int i = 0; i < 9; i++) {
            int r = i / 3;
            int col = i % 3;
            double x = col * (cs + 3);
            double y = r  * (cs + 3);

            // Cell bg
            boolean isWin = winSet.contains(i);
            gc.setFill(isWin ? Color.web("#1a2a1a") : Color.web("#13132a"));
            gc.fillRoundRect(x, y, cs, cs, 4, 4);

            // Symbol
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

        // Win line overlay
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
        int col = idx % 3;
        int row = idx / 3;
        return new double[]{col * (cs + 3) + cs / 2, row * (cs + 3) + cs / 2};
    }

    // ── Game logic helpers ────────────────────────────────────────────────────
    private void restartGame() {
        // Cancel any running draw animations
        for (int i = 0; i < 9; i++) {
            if (drawAnims[i] != null) drawAnims[i].stop();
        }

        game.reset();
        gameStarted = false;

        // Remove any win-line overlays added to the board wrapper
        StackPane wrapper = getBoardWrapper();
        if (wrapper != null) {
            wrapper.getChildren().removeIf(n -> n instanceof Line);
        }

        // Rebuild board visuals
        buildBoard();

        difficultySlider.setDisable(false);
        updateStatus();

        if (vsAI && humanPlayer == Player.O) scheduleAiMove(900);
    }

    private void lockDifficultyIfFirstMove() {
        if (!gameStarted) {
            gameStarted = true;
            difficultySlider.setDisable(true);
        }
    }

    private void scheduleAiMove(long delayMs) {
        // Disable all overlays
        for (Region ov : overlays) { if (ov != null) ov.setMouseTransparent(true); }

        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> Platform.runLater(() -> {
            if (game.isGameOver()) return;

            // Re-enable clickable cells
            for (int i = 0; i < 9; i++) {
                if (overlays[i] != null) overlays[i].setMouseTransparent(false);
            }

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
                if (vsAI) {
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
        // Small delay so the final symbol animation can complete first
        PauseTransition pt = new PauseTransition(Duration.millis(320));
        pt.setOnFinished(e -> {
            int[] wl = game.getWinningLine();
            if (wl != null) {
                animateWinLine(wl);
            }
        });
        pt.play();
    }

    private void freezeBoard() {
        for (Region ov : overlays) {
            if (ov != null) ov.setMouseTransparent(true);
        }
    }

    private void setStatus(String text, String hexColor) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 26px; -fx-font-weight: bold;");
    }

    private String resolveWinner(Player w) {
        if (!vsAI) return "Spieler " + w;
        return (w == humanPlayer) ? "Du" : "KI";
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }
}