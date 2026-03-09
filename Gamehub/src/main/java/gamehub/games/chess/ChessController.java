package gamehub.games.chess;

import gamehub.gamehub.GamehubApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class ChessController {

    @FXML
    private StackPane chessBoardPane;

    @FXML
    public void initialize() {
        Board board = new Board();
        StackPane.setAlignment(board, javafx.geometry.Pos.CENTER);
        chessBoardPane.getChildren().add(board);
    }

    @FXML
    protected void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("main-menu.fxml")
            );
            chessBoardPane.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}