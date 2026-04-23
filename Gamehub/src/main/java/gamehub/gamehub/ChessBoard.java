package gamehub.gamehub;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.Check;
import gamehub.games.chess.Figure;
import gamehub.games.chess.figures.*;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChessBoard extends GridPane {

    public static final int SQUARE_SIZE = 90;
    private static final int COLS = 8;
    private static final int ROWS = 8;
    private ChessController controller;
    private BoardLogic logic = new BoardLogic();
    private Figure selectedFigure = null;

    public ChessBoard() {
        setAlignment(javafx.geometry.Pos.CENTER);
        draw();
    }

    public void setController(ChessController controller) {
        this.controller = controller;
    }

    public BoardLogic getLogic() {
        return logic;
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

    public void placeFigure(Figure figure) {
        logic.addFigure(figure);
        figure.setOnMouseClicked(event -> {
            handleClick(figure.col, figure.row);
            event.consume(); // verhindert das der Klick ans Feld weitergegeben wird
        });
        add(figure, figure.col + 1, figure.row);
    }

    private void handleClick(int col, int row) {
        Figure clickedFigure = logic.getFigureAt(col, row);
        String turncolour;
        if (logic.whiteTurn) {
            turncolour = "w";
        } else {
            turncolour = "b";
        }
        if (clickedFigure != null && clickedFigure.type.startsWith(turncolour)) {
            if (selectedFigure != null) {
                highlightSquare(selectedFigure.col, selectedFigure.row, false);
            }
            selectedFigure = clickedFigure;
            highlightSquare(selectedFigure.col, selectedFigure.row, true);
            clearPossibleMoves();
            showPossibleMoves(selectedFigure);

        } else if (selectedFigure != null) {
            int oldCol = selectedFigure.col;
            int oldRow = selectedFigure.row;

            if (selectedFigure.canMoveTo(col, row, getLogic())) {
                Figure targetFigure = logic.getFigureAt(col, row);
                selectedFigure.col = col;
                selectedFigure.row = row;
                if (targetFigure != null) {
                    targetFigure.col = -1;
                    targetFigure.row = -1;
                }

                boolean illegal = Check.isInCheck(turncolour, getLogic());

                // Zurücksetzen
                selectedFigure.col = oldCol;
                selectedFigure.row = oldRow;
                if (targetFigure != null) {
                    targetFigure.col = col;
                    targetFigure.row = row;
                }
                if (!illegal) {
                    clearPossibleMoves();
                    moveFigure(selectedFigure, col, row);
                    logic.whiteTurn = !logic.whiteTurn;
                    controller.updateTurnDisplay(logic.whiteTurn);
                }
                highlightSquare(oldCol, oldRow, false);
                highlightSquare(col, row, false);
                clearPossibleMoves();
                selectedFigure = null;
            }
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
        Figure enPassantPawn = null;
        if (figure instanceof Pawn && targetCol != figure.col && logic.getFigureAt(targetCol, targetRow) == null) {
            enPassantPawn = logic.enPassantTarget;
        }
        logic.enPassantTarget = null;
        int oldCol = figure.col; //für Rochade
        int oldRow = figure.row; //für enPassant
        Figure targetFigure = logic.getFigureAt(targetCol, targetRow);
        if (targetFigure != null) {
            getChildren().remove(targetFigure);
            logic.removeFigure(targetFigure);
            logic.addCapturedFigure(targetFigure);
            controller.updateCapturedFiguresUI();

        }
        if (enPassantPawn != null) {
            getChildren().remove(enPassantPawn);
            logic.removeFigure(enPassantPawn);
        }
        getChildren().remove(figure);
        figure.col = targetCol;
        figure.row = targetRow;
        add(figure, targetCol + 1, targetRow);
        controller.recordMove(oldCol, oldRow, targetCol, targetRow, figure.getClass().getSimpleName());

        if (figure instanceof Pawn && Math.abs(targetRow - oldRow) == 2) {
            logic.enPassantTarget = figure;
        }

        if (figure instanceof King && Math.abs(targetCol - oldCol) == 2) {
            if (targetCol == 6) {
                moveRookUI(7, 5, targetRow);
            } else if (targetCol == 2) {
                moveRookUI(0, 3, targetRow);
            }
        }
        if (figure instanceof Pawn && ((Pawn) figure).isPromotionRow(targetRow)) {
            String colourCode = figure.type.substring(0, 1);
            String choice = controller.showPromotionDialog(colourCode);
            promotePawn(figure, targetCol, targetRow, choice);
        }
        String enemyColour = null;
        if (logic.whiteTurn) {
            enemyColour = "b";
        } else {
            enemyColour = "w";
        }
        if (Check.isStalemate(enemyColour, getLogic())) {
            controller.showPattDialog();
        }
        if (Check.isInsufficientMaterial(getLogic())) {
            controller.showPattDialog();
        }
        if (Check.isInCheck(enemyColour, getLogic())) {
            if (Check.isCheckmate(enemyColour, getLogic())) {
                String winner = null;
                if (logic.whiteTurn) {
                    winner = "Weiß";
                } else {
                    winner = "Schwarz";
                }
                controller.showCheckmateDialog(winner);
            }
            controller.showCheckMessage("SCHACH!", true);
        } else {
            controller.showCheckMessage("", false);
        }
        figure.hasMoved = true;
    }

    private void promotePawn(Figure pawn, int col, int row, String choice) {
        // Alten Bauern vom Brett nehmen
        getChildren().remove(pawn);
        logic.removeFigure(pawn);

        String color = pawn.type.substring(0, 1);
        Figure newFigure;

        if (choice.equals("Turm")) {
            newFigure = new Rook(color, col, row);
        } else if (choice.equals("Läufer")) {
            newFigure = new Bishop(color, col, row);
        } else if (choice.equals("Springer")) {
            newFigure = new Knight(color, col, row);
        } else {
            newFigure = new Queen(color, col, row);
        }
        placeFigure(newFigure);
    }

    private void moveRookUI(int oldCol, int newCol, int row) {
        Figure rook = logic.getFigureAt(oldCol, row);
        if (rook != null) {
            getChildren().remove(rook);
            logic.moveRookLogic(rook, newCol, row); // BoardLogic-Werte ändern
            add(rook, newCol + 1, row); // UI-Position ändern
        }
    }

    private void showPossibleMoves(Figure figure) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (figure.canMoveTo(col, row, logic)) {
                    // Schach-Check: wäre der Zug legal?
                    int oldCol = figure.col;
                    int oldRow = figure.row;
                    Figure target = logic.getFigureAt(col, row);
                    figure.col = col;
                    figure.row = row;
                    if (target != null) {
                        target.col = -1;
                        target.row = -1;
                    }
                    boolean illegal = Check.isInCheck(figure.type.substring(0, 1), logic);
                    figure.col = oldCol;
                    figure.row = oldRow;
                    if (target != null) {
                        target.col = col;
                        target.row = row;
                    }

                    if (!illegal) {
                        Circle dot = new Circle(15);
                        dot.setFill(Color.rgb(0, 0, 0, 0.25));
                        dot.setMouseTransparent(true); // Klicks durchlassen
                        dot.setId("moveDot");
                        GridPane.setHalignment(dot, HPos.CENTER);
                        GridPane.setValignment(dot, VPos.CENTER);
                        add(dot, col + 1, row);
                    }
                }
            }
        }
    }
    private void clearPossibleMoves() {
        getChildren().removeIf(node -> "moveDot".equals(node.getId()));
    }
}