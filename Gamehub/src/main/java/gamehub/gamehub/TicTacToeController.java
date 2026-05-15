package gamehub.gamehub;

import gamehub.games.ticTacToe.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class TicTacToeController {

    // ── Styles ────────────────────────────────────────────────────────────────
    private static final String STYLE_CELL_DEFAULT = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #778CA1; -fx-text-fill: white; -fx-cursor: hand;";
    private static final String STYLE_CELL_PLAYED  = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #dce3e8; -fx-cursor: default; -fx-opacity: 1.0;";
    private static final String STYLE_CELL_WINNING = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #f0a500; -fx-text-fill: white; -fx-cursor: default; -fx-opacity: 1.0;";
    private static final String STYLE_X_COLOR      = "-fx-text-fill: #1a1a1a;";
    private static final String STYLE_O_COLOR      = "-fx-text-fill: #a80000;";

    private static final String STYLE_SIDE_BTN_ACTIVE   =
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #4a90d9; " +
                    "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 24;";
    private static final String STYLE_SIDE_BTN_INACTIVE =
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #3a3a5c; " +
                    "-fx-text-fill: #aaaaaa; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 24;";

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private GridPane boardGrid;
    @FXML private Label    statusLabel;

    // AI-mode controls (only visible in AI mode)
    @FXML private HBox   sideSelectionBox;   // container for the two side buttons
    @FXML private Button playerSideBtn;      // shows "Spieler  X" or "Spieler  O"
    @FXML private Button aiSideBtn;          // shows "KI  O" or "KI  X"
    @FXML private VBox   difficultyBox;      // container for slider + label
    @FXML private Slider difficultySlider;
    @FXML private Label  difficultyLabel;

    // Board buttons
    @FXML private Button cell0;
    @FXML private Button cell1;
    @FXML private Button cell2;
    @FXML private Button cell3;
    @FXML private Button cell4;
    @FXML private Button cell5;
    @FXML private Button cell6;
    @FXML private Button cell7;
    @FXML private Button cell8;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Button> cells;
    private TicTacToeGame game;

    /** true  → playing against the AI, false → two human players */
    private boolean vsAI = false;

    /**
     * The player that the HUMAN controls.
     * X always goes first; if humanPlayer == O then AI moves first.
     */
    private Player humanPlayer = Player.X;

    /** Difficulty 0–100 (only meaningful when vsAI == true). */
    private int difficulty = 100;

    /** Whether the first move of the current game has already been made. */
    private boolean gameStarted = false;

    // ── Public API called by MainMenuController ───────────────────────────────

    /**
     * Called by MainMenuController before the scene is shown.
     *
     * @param vsAI true → player vs AI, false → player vs player
     */
    public void initMode(boolean vsAI) {
        this.vsAI = vsAI;
    }

    // ── FXML lifecycle ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        cells = List.of(cell0, cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8);
        game  = new TicTacToeGame();
    }

    /**
     * Called after the FXML fields are injected AND after initMode() has been
     * called.  We use a post-layout hook via the scene property so that
     * sideSelectionBox / difficultyBox are guaranteed to exist.
     *
     * Alternatively, call this explicitly from MainMenuController after load().
     */
    public void postInit() {
        if (vsAI) {
            sideSelectionBox.setVisible(true);
            sideSelectionBox.setManaged(true);
            difficultyBox.setVisible(true);
            difficultyBox.setManaged(true);
        } else {
            sideSelectionBox.setVisible(false);
            sideSelectionBox.setManaged(false);
            difficultyBox.setVisible(false);
            difficultyBox.setManaged(false);
        }

        // Difficulty slider
        difficultySlider.setMin(0);
        difficultySlider.setMax(100);
        difficultySlider.setValue(difficulty);
        difficultySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            difficulty = newVal.intValue();
            difficultyLabel.setText(TicTacToeAI.difficultyLabel(difficulty));
        });
        difficultyLabel.setText(TicTacToeAI.difficultyLabel(difficulty));

        refreshSideButtons();
        updateStatus();

        // If AI plays X, trigger its first move after a short delay
        if (vsAI && humanPlayer == Player.O) {
            scheduleAiMove(800);
        }
    }

    // ── Side-selection buttons ────────────────────────────────────────────────

    @FXML
    protected void onPlayerSideBtnClick() {
        // Toggle: if player is already X, switch to O; otherwise switch to X
        humanPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
        refreshSideButtons();
        restartGame();
    }

    @FXML
    protected void onAiSideBtnClick() {
        // Mirror of the player button
        humanPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
        refreshSideButtons();
        restartGame();
    }

    private void refreshSideButtons() {
        String playerSymbol = (humanPlayer == Player.X) ? "X" : "O";
        String aiSymbol     = (humanPlayer == Player.X) ? "O" : "X";

        playerSideBtn.setText("Spieler   " + playerSymbol);
        aiSideBtn.setText("KI   " + aiSymbol);

        // Highlight whoever plays X (goes first) as "active"
        if (humanPlayer == Player.X) {
            playerSideBtn.setStyle(STYLE_SIDE_BTN_ACTIVE);
            aiSideBtn.setStyle(STYLE_SIDE_BTN_INACTIVE);
        } else {
            playerSideBtn.setStyle(STYLE_SIDE_BTN_INACTIVE);
            aiSideBtn.setStyle(STYLE_SIDE_BTN_ACTIVE);
        }
    }

    // ── Cell click ────────────────────────────────────────────────────────────

    @FXML
    protected void onCellClick(javafx.event.ActionEvent event) {
        // In AI mode, ignore clicks when it's not the human's turn
        if (vsAI && game.getCurrentPlayer() != humanPlayer) return;

        Button clicked = (Button) event.getSource();
        int index = cells.indexOf(clicked);

        if (!game.makeMove(index)) return;

        lockDifficultyIfFirstMove();
        renderMove(index);
        updateStatus();

        if (!game.isGameOver() && vsAI) {
            scheduleAiMove(400);
        }
    }

    // ── Restart / Back ────────────────────────────────────────────────────────

    @FXML
    protected void onRestartClick() {
        restartGame();
    }

    @FXML
    protected void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("main-menu.fxml")
            );
            boardGrid.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Full game reset – also re-enables the difficulty slider. */
    private void restartGame() {
        game.reset();
        gameStarted = false;

        for (Button cell : cells) {
            cell.setText("");
            cell.setDisable(false);
            cell.setStyle(STYLE_CELL_DEFAULT);
        }

        // Re-enable difficulty slider
        difficultySlider.setDisable(false);

        updateStatus();

        // If AI is X, let it move after the misclick-buffer delay
        if (vsAI && humanPlayer == Player.O) {
            scheduleAiMove(800);
        }
    }

    private void lockDifficultyIfFirstMove() {
        if (!gameStarted) {
            gameStarted = true;
            difficultySlider.setDisable(true);
        }
    }

    /** Renders a move that has already been applied to the game model. */
    private void renderMove(int index) {
        Player player = game.getCell(index);
        Button cell   = cells.get(index);
        cell.setText(player == Player.X ? "X" : "O");
        cell.setStyle(STYLE_CELL_PLAYED + (player == Player.X ? STYLE_X_COLOR : STYLE_O_COLOR));
        cell.setDisable(true);
    }

    /** Schedules an AI move after {@code delayMs} milliseconds. */
    private void scheduleAiMove(long delayMs) {
        // Block human input while AI is "thinking"
        cells.forEach(c -> c.setDisable(true));

        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> Platform.runLater(() -> {
            if (game.isGameOver()) return;

            // Re-enable only empty cells before the AI moves
            for (int i = 0; i < 9; i++) {
                if (game.getCell(i) == null) cells.get(i).setDisable(false);
            }

            Player aiPlayer = (humanPlayer == Player.X) ? Player.O : Player.X;
            int move = TicTacToeAI.getBestMove(game.getBoard(), aiPlayer, difficulty);
            if (move == -1) return;

            game.makeMove(move);
            lockDifficultyIfFirstMove();
            renderMove(move);
            updateStatus();

            if (game.isGameOver()) {
                disableAllCells();
            }
        }));
        pause.play();
    }

    private void updateStatus() {
        switch (game.getGameState()) {
            case X_WINS -> {
                String winner = resolveWinnerLabel(Player.X);
                statusLabel.setText(winner + " gewinnt!");
                highlightWinningLine();
                disableAllCells();
            }
            case O_WINS -> {
                String winner = resolveWinnerLabel(Player.O);
                statusLabel.setText(winner + " gewinnt!");
                highlightWinningLine();
                disableAllCells();
            }
            case DRAW -> {
                statusLabel.setText("Unentschieden!");
                disableAllCells();
            }
            case PLAYING -> {
                Player current = game.getCurrentPlayer();
                String label   = vsAI
                        ? (current == humanPlayer ? "Du bist dran (" + current + ")" : "KI denkt… (" + current + ")")
                        : "Spieler " + current + " ist an der Reihe";
                statusLabel.setText(label);
                statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
            }
        }
    }

    /** Returns "Du" / "KI" or "Spieler X/O" depending on mode. */
    private String resolveWinnerLabel(Player winner) {
        if (!vsAI) return "Spieler " + winner;
        return (winner == humanPlayer) ? "Du" : "KI";
    }

    private void highlightWinningLine() {
        int[] line = game.getWinningLine();
        if (line == null) return;
        for (int i : line) cells.get(i).setStyle(STYLE_CELL_WINNING);
    }

    private void disableAllCells() {
        cells.forEach(c -> c.setDisable(true));
    }
}