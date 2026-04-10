package gamehub.games.ticTacToe;

import gamehub.gamehub.GamehubApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.List;

public class TicTacToeController {

    private static final String STYLE_CELL_DEFAULT = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #778CA1; -fx-text-fill: white; -fx-cursor: hand;";
    private static final String STYLE_CELL_PLAYED  = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #dce3e8; -fx-cursor: default; -fx-opacity: 1.0;";
    private static final String STYLE_CELL_WINNING = "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: #f0a500; -fx-text-fill: white; -fx-cursor: default; -fx-opacity: 1.0;";
    private static final String STYLE_X_COLOR      = "-fx-text-fill: #1a1a1a;";
    private static final String STYLE_O_COLOR      = "-fx-text-fill: #a80000;";

    @FXML private GridPane boardGrid;
    @FXML private Label statusLabel;

    @FXML private Button cell0;
    @FXML private Button cell1;
    @FXML private Button cell2;
    @FXML private Button cell3;
    @FXML private Button cell4;
    @FXML private Button cell5;
    @FXML private Button cell6;
    @FXML private Button cell7;
    @FXML private Button cell8;

    private List<Button> cells;
    private TicTacToeGame game;

    @FXML
    public void initialize() {
        cells = List.of(cell0, cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8);
        game = new TicTacToeGame();
    }

    @FXML
    protected void onCellClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        int index = cells.indexOf(clicked);

        if (!game.makeMove(index)) return;

        Player player = game.getCell(index);
        clicked.setText(player == Player.X ? "X" : "O");
        clicked.setStyle(STYLE_CELL_PLAYED + (player == Player.X ? STYLE_X_COLOR : STYLE_O_COLOR));
        clicked.setDisable(true);

        updateStatus();
    }

    @FXML
    protected void onRestartClick() {
        game.reset();
        for (Button cell : cells) {
            cell.setText("");
            cell.setDisable(false);
            cell.setStyle(STYLE_CELL_DEFAULT);
        }
        statusLabel.setText("Spieler X ist an der Reihe");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
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
            case X_WINS -> {
                statusLabel.setText("Spieler X gewinnt!");
                highlightWinningLine();
                disableAllCells();
            }
            case O_WINS -> {
                statusLabel.setText("Spieler O gewinnt!");
                highlightWinningLine();
                disableAllCells();
            }
            case DRAW -> {
                statusLabel.setText("Unentschieden!");
                disableAllCells();
            }
            case PLAYING -> {
                String next = game.getCurrentPlayer() == Player.X ? "X" : "O";
                statusLabel.setText("Spieler " + next + " ist an der Reihe");
            }
        }
    }

    private void highlightWinningLine() {
        int[] line = game.getWinningLine();
        if (line == null) return;
        for (int i : line) {
            cells.get(i).setStyle(STYLE_CELL_WINNING);
        }
    }

    private void disableAllCells() {
        cells.forEach(c -> c.setDisable(true));
    }
}