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
        board.placeFigure(new Figure("wK",4,7)); board.placeFigure(new Figure("bK",4,0));
        board.placeFigure(new Figure("wQ",3,7)); board.placeFigure(new Figure("bQ",3,0));
        board.placeFigure(new Figure("wB",2,7)); board.placeFigure(new Figure("wB",5,7));
        board.placeFigure(new Figure("bB",2,0)); board.placeFigure(new Figure("bB",5,0));
        board.placeFigure(new Figure("wN",1,7)); board.placeFigure(new Figure("wN",6,7));
        board.placeFigure(new Figure("bN",1,0)); board.placeFigure(new Figure("bN",6,0));
        board.placeFigure(new Figure("wR",0,7)); board.placeFigure(new Figure("wR",7,7));
        board.placeFigure(new Figure("bR",0,0)); board.placeFigure(new Figure("bR",7,0));

        board.placeFigure(new Pawn("w",0,6)); board.placeFigure(new Pawn("w",1,6));
        board.placeFigure(new Pawn("w",2,6)); board.placeFigure(new Pawn("w",3,6));
        board.placeFigure(new Pawn("w",4,6)); board.placeFigure(new Pawn("w",5,6));
        board.placeFigure(new Pawn("w",6,6)); board.placeFigure(new Pawn("w",7,6));

        board.placeFigure(new Pawn("b",0,1)); board.placeFigure(new Pawn("b",1,1));
        board.placeFigure(new Pawn("b",2,1)); board.placeFigure(new Pawn("b",3,1));
        board.placeFigure(new Pawn("b",4,1)); board.placeFigure(new Pawn("b",5,1));
        board.placeFigure(new Pawn("b",6,1)); board.placeFigure(new Pawn("b",7,1));
    }
}