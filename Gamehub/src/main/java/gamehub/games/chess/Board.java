package gamehub.games.chess;

import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Board extends GridPane {

    public static final int SQUARE_SIZE = 65;
    private static final int COLS = 8;
    private static final int ROWS = 8;

    public Board() {
        setAlignment(javafx.geometry.Pos.CENTER);
        draw();
    }

    public void draw() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Rectangle square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                if ((row + col) % 2 == 0) {
                    square.setFill(Color.rgb(255, 240, 255));
                } else {
                    square.setFill(Color.rgb(78, 120, 55));
                }
                add(square, col, row);
            }
        }
    }
}