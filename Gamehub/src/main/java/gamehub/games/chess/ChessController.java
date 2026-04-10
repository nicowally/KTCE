package gamehub.games.chess;

import gamehub.games.chess.figures.*;
import gamehub.gamehub.GamehubApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class ChessController {

    @FXML
    private StackPane chessBoardPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Rectangle turnIndicator;

    @FXML
    public void initialize() {
        Board board = new Board();
        StackPane.setAlignment(board, javafx.geometry.Pos.CENTER);
        chessBoardPane.getChildren().add(board);
        setStartPosition(board);
        board.setController(this);
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

    public void updateTurnDisplay(boolean whiteTurn) {
        if(whiteTurn) {
            statusLabel.setText("Weiß ist am Zug");
            turnIndicator.setFill(Color.WHITE);
        } else {
            statusLabel.setText("Schwarz ist am Zug");
            turnIndicator.setFill(Color.BLACK);
        }
    }
    protected void setStartPosition(Board board) {
        board.placeFigure(new King("w",4,7)); board.placeFigure(new King("b",4,0));

        board.placeFigure(new Queen("w",3,7)); board.placeFigure(new Queen("b",3,0));

        board.placeFigure(new Bishop("w",2,7)); board.placeFigure(new Bishop("w",5,7));
        board.placeFigure(new Bishop("b",2,0)); board.placeFigure(new Bishop("b",5,0));

        board.placeFigure(new Knight("w",1,7)); board.placeFigure(new Knight("w",6,7));
        board.placeFigure(new Knight("b",1,0)); board.placeFigure(new Knight("b",6,0));

        board.placeFigure(new Rook("w",0,7)); board.placeFigure(new Rook("w",7,7));
        board.placeFigure(new Rook("b",0,0)); board.placeFigure(new Rook("b",7,0));

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