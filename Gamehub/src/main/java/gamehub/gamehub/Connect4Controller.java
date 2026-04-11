package gamehub.gamehub;

import gamehub.games.connectFour.Connect4Game;
import gamehub.games.connectFour.Connect4Player;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Connect4Controller {

    private static final Color COLOR_EMPTY   = Color.web("#2c3e50");
    private static final Color COLOR_RED     = Color.web("#e74c3c");
    private static final Color COLOR_YELLOW  = Color.web("#f1c40f");
    private static final Color COLOR_WIN     = Color.web("#ffffff");
    private static final double DISC_RADIUS  = 32.0;

    @FXML private GridPane boardGrid;
    @FXML private Label statusLabel;

    // 7 drop buttons at the top of each column
    @FXML private Button col0;
    @FXML private Button col1;
    @FXML private Button col2;
    @FXML private Button col3;
    @FXML private Button col4;
    @FXML private Button col5;
    @FXML private Button col6;

    private java.util.List<Button> colButtons;
    private Circle[][] circles; // [row][col]
    private Connect4Game game;

    @FXML
    public void initialize() {
        colButtons = java.util.List.of(col0, col1, col2, col3, col4, col5, col6);
        game = new Connect4Game();
        circles = new Circle[Connect4Game.ROWS][Connect4Game.COLS];

        // Build the disc grid
        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                Circle disc = new Circle(DISC_RADIUS, COLOR_EMPTY);
                disc.setStroke(Color.web("#1a1a2e"));
                disc.setStrokeWidth(2);
                circles[r][c] = disc;

                StackPane cell = new StackPane(disc);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle("-fx-padding: 4;");
                boardGrid.add(cell, c, r);
            }
        }

        updateStatus();
    }

    @FXML
    protected void onColumnClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        int col = colButtons.indexOf(clicked);

        int row = game.dropDisc(col);
        if (row == -1) return; // full column or game over

        // Paint the disc
        Connect4Player player = game.getCell(row, col);
        circles[row][col].setFill(player == Connect4Player.RED ? COLOR_RED : COLOR_YELLOW);

        // Disable full columns
        if (!game.isColumnPlayable(col)) {
            clicked.setDisable(true);
        }

        updateStatus();

        if (game.isGameOver()) {
            highlightWinningCells();
            disableAllColumns();
        }
    }

    @FXML
    protected void onRestartClick() {
        game.reset();

        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                circles[r][c].setFill(COLOR_EMPTY);
                circles[r][c].setStroke(Color.web("#1a1a2e"));
                circles[r][c].setStrokeWidth(2);
                circles[r][c].setOpacity(1.0);
            }
        }

        for (Button b : colButtons) {
            b.setDisable(false);
        }
        updateStatus();
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

    private void updateStatus() {
        switch (game.getGameState()) {
            case RED_WINS    -> setStatus("🔴  Rot gewinnt!", "#e74c3c");
            case YELLOW_WINS -> setStatus("🟡  Gelb gewinnt!", "#f1c40f");
            case DRAW        -> setStatus("Unentschieden!", "white");
            case PLAYING     -> {
                if (game.getCurrentPlayer() == Connect4Player.RED) {
                    setStatus("🔴  Rot ist dran", "#e74c3c");
                } else {
                    setStatus("🟡  Gelb ist dran", "#f1c40f");
                }
            }
        }
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
    }

    private void highlightWinningCells() {
        int[] cells = game.getWinningCells();
        if (cells == null) return;
        Set<Integer> winSet = new HashSet<>();
        for (int flat : cells) winSet.add(flat);

        for (int r = 0; r < Connect4Game.ROWS; r++) {
            for (int c = 0; c < Connect4Game.COLS; c++) {
                int flat = r * Connect4Game.COLS + c;
                if (winSet.contains(flat)) {
                    circles[r][c].setStroke(COLOR_WIN);
                    circles[r][c].setStrokeWidth(4);
                } else if (circles[r][c].getFill() != COLOR_EMPTY) {
                    circles[r][c].setOpacity(0.4);
                }
            }
        }
    }

    private void disableAllColumns() {
        colButtons.forEach(b -> b.setDisable(true));
    }
}
