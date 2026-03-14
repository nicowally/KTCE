package gamehub.games.chess;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Board extends GridPane {

    public static final int SQUARE_SIZE = 90;
    private static final int COLS = 8;
    private static final int ROWS = 8;
    private Figure selectedFigure = null;
    private boolean whiteTurn = true;
    private ChessController controller;

    public Board() {
        setAlignment(javafx.geometry.Pos.CENTER);
        draw();
    }
    public void setController(ChessController controller) {
        this.controller =controller;
    }

    public void draw() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Rectangle square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                if ((row + col) % 2 == 0) {
                    square.setFill(Color.rgb(255, 255, 255));
                } else {
                    square.setFill(Color.rgb(78, 120, 55));
                }
                int c = col;
                int r = row;
                square.setOnMouseClicked(event -> handleClick(c, r));
                add(square, col + 1, row); //+1 damit die Zahlen links vom Board später richtig positioniert werden
            }
        }
        //Zahlen links vom Board
        for (int row = 0; row < ROWS; row++) {
            Label label = new Label(String.valueOf(8 - row));
            label.setFont(Font.font("System", FontWeight.BOLD, 16));
            label.setTextFill(Color.WHITE);
            label.setMinSize(30, SQUARE_SIZE);
            label.setAlignment(Pos.CENTER);
            add(label, 0, row);
        }

        // Buchstaben unterhalb vom Board
        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (int col = 0; col < COLS; col++) {
            Label label = new Label(letters[col]);
            label.setFont(Font.font("System", FontWeight.BOLD, 16));
            label.setTextFill(Color.WHITE);
            label.setMinSize(SQUARE_SIZE, 30);
            label.setAlignment(Pos.CENTER);
            add(label, col + 1, ROWS);
        }
    }

    protected void placeFigure(Figure figure) {
        figure.setOnMouseClicked(event -> {
            handleClick(figure.col, figure.row);
            event.consume(); // verhindert das der Klick ans Feld weitergegeben wird
        });
        add(figure, figure.col+1, figure.row);
    }

    public Figure getFigureAt(int col, int row) {
        return getChildren().stream()
                .filter(node -> node instanceof Figure f && f.col == col && f.row == row)
                .map(node -> (Figure) node)
                .findFirst()
                .orElse(null);
    }
    private void handleClick(int col, int row) {
        Figure clickedFigure = getFigureAt(col, row);
        String turncolour;
        if(whiteTurn) {
            turncolour = "w";
        } else {
            turncolour = "b";
        }
        if (selectedFigure == null) {
            if (clickedFigure != null) {
                if(clickedFigure.type.startsWith(turncolour)){
                    selectedFigure = clickedFigure;
                    highlightSquare(selectedFigure.col, selectedFigure.row, true);
                }
            }
        } else {

            int oldCol = selectedFigure.col;
            int oldRow = selectedFigure.row;

            if (selectedFigure.canMoveTo(col, row, this)) {
                moveFigure(selectedFigure, col, row);
                whiteTurn = !whiteTurn;
                controller.updateTurnDisplay(whiteTurn);

            }
            highlightSquare(oldCol, oldRow, false);
            highlightSquare(col,row, false);
            selectedFigure = null;
        }
    }
    private void highlightSquare(int col, int row, boolean highlight) {
        for (Node node : getChildren()) {
            if (node instanceof Rectangle rect && GridPane.getColumnIndex(rect) == col + 1 && GridPane.getRowIndex(rect) == row) {
                if (highlight) {
                    rect.setFill(Color.rgb(247, 247, 105));
                } else {
                    if ((row + col) % 2 == 0) {
                        rect.setFill(Color.rgb(255, 255, 255));
                    } else {
                        rect.setFill(Color.rgb(78, 120, 55));
                    }
                }
            }
        }
    }
    public void moveFigure(Figure figure, int targetCol, int targetRow) {
        Figure targetFigure = getFigureAt(targetCol, targetRow);
        if (targetFigure != null) {
            getChildren().remove(targetFigure);
        }
        getChildren().remove(figure);
        figure.col = targetCol;
        figure.row = targetRow;
        add(figure, targetCol + 1, targetRow);
    }

    public boolean isPathClear(int startCol, int startRow, int targetCol, int targetRow) {
        int diffCol = Integer.compare(targetCol, startCol); // Entweder -1, 0 oder 1
        int diffRow = Integer.compare(targetRow, startRow); // Entweder -1, 0 oder 1

        int currentCol = startCol + diffCol;
        int currentRow = startRow + diffRow;

        while (currentCol != targetCol || currentRow != targetRow) {
            if (getFigureAt(currentCol, currentRow) != null) {
                return false;
            }
            currentCol += diffCol;
            currentRow += diffRow;
        }
        return true;
    }
}