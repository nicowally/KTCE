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
        setStartPosition(board);
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
    protected void setStartPosition(Board board) {
        board.placeFigure(new figure("wK",4,7)); board.placeFigure(new figure("bK",4,0));
        board.placeFigure(new figure("wQ",3,7)); board.placeFigure(new figure("bQ",3,0));
        board.placeFigure(new figure("wB",2,7)); board.placeFigure(new figure("wB",5,7));
        board.placeFigure(new figure("bB",2,0)); board.placeFigure(new figure("bB",5,0));
        board.placeFigure(new figure("wN",1,7)); board.placeFigure(new figure("wN",6,7));
        board.placeFigure(new figure("bN",1,0)); board.placeFigure(new figure("bN",6,0));
        board.placeFigure(new figure("wR",0,7)); board.placeFigure(new figure("wR",7,7));
        board.placeFigure(new figure("bR",0,0)); board.placeFigure(new figure("bR",7,0));

        board.placeFigure(new figure("wP",0,6)); board.placeFigure(new figure("wP",1,6));
        board.placeFigure(new figure("wP",2,6)); board.placeFigure(new figure("wP",3,6));
        board.placeFigure(new figure("wP",4,6)); board.placeFigure(new figure("wP",5,6));
        board.placeFigure(new figure("wP",6,6)); board.placeFigure(new figure("wP",7,6));

        board.placeFigure(new figure("bP",0,1)); board.placeFigure(new figure("bP",1,1));
        board.placeFigure(new figure("bP",2,1)); board.placeFigure(new figure("bP",3,1));
        board.placeFigure(new figure("bP",4,1)); board.placeFigure(new figure("bP",5,1));
        board.placeFigure(new figure("bP",6,1)); board.placeFigure(new figure("bP",7,1));
    }
}