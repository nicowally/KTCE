package gamehub.games.chess;

import gamehub.games.chess.figures.*;
import java.util.ArrayList;
import java.util.List;

public class BoardLogic {

    private List<Figure> figures = new ArrayList<>();
    public Figure enPassantTarget = null;
    public boolean whiteTurn = true;
    private List<Figure> capturedFigures = new ArrayList<>();

    public void addCapturedFigure(Figure figure) {
        capturedFigures.add(figure);
    }

    public void addFigure(Figure figure) {
        figures.add(figure);
    }
    public void removeFigure(Figure figure) {
        figures.remove(figure);
    }
    public List<Figure> getFigures() {
        return figures;
    }
    public Figure getEnPassantTarget() {
        return enPassantTarget;
    }
    public void setEnPassantTarget(Figure figure) {
        this.enPassantTarget = figure;
    }

    public Figure getFigureAt(int col, int row) {
        return figures.stream()
                .filter(figure -> figure.col == col && figure.row == row)
                .findFirst()
                .orElse(null);
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
    public void moveRookLogic(Figure rook, int newCol, int newRow) {
        if (rook != null) {
            rook.col = newCol;
            rook.row = newRow;
            rook.hasMoved = true;
        }
    }
    public int getCapturedFigureCount(String color, String type) {
        int count = 0;
        String searchKey = color + type; // Ergibt zum Beispiel "wP"
        for (Figure f : capturedFigures) {
            if (f.type.equals(searchKey)) {
                count++;
            }
        }
        return count;
    }
}