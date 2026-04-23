package gamehub.games.chess;

import gamehub.games.chess.figures.Bishop;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Knight;

import java.util.List;

public class Check {

    public static boolean isInCheck(String colour, BoardLogic logic) {
        King king = null;
        for (Figure figure : logic.getFigures()) {
            if (figure instanceof King k && k.type.startsWith(colour)) {
                king = k;
                break;
            }
        }
        if (king == null) return false;

        String enemyColour = null;
        if (colour.equals("w")) {
            enemyColour = "b";
        } else
            enemyColour = "w";
        for (Figure figure : logic.getFigures()) {
            if (figure.type.startsWith(enemyColour)) {
                if (figure.canMoveTo(king.col, king.row, logic)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCheckmate(String colour, BoardLogic logic) {
        if (!isInCheck(colour, logic)) {
            return false;
        }
        //Liste kopieren zur sicherheit
        List<Figure> myFigures = logic.getFigures().stream()
                .filter(f -> f.type.startsWith(colour))
                .toList();

        for (Figure figure : myFigures) {
            if (figure.type.startsWith(colour)) {

                // jedes feld auf dem Brett prüfen
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {

                        // Kann Figur dort hin?
                        if (figure.canMoveTo(col, row, logic)) {

                            // Simulation - wäre Schach dann weg
                            int oldCol = figure.col;
                            int oldRow = figure.row;
                            Figure target = logic.getFigureAt(col, row);

                            figure.col = col;
                            figure.row = row;
                            if (target != null) {
                                target.col = -1;
                                target.row = -1;
                            }

                            boolean stillInCheck = isInCheck(colour, logic);

                            // Zurücksetzen
                            figure.col = oldCol;
                            figure.row = oldRow;
                            if (target != null) {
                                target.col = col;
                                target.row = row;
                            }
                            // Wenn es Zug gibt - Kein Schachmatt
                            if (!stillInCheck) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isStalemate(String colour, BoardLogic logic) {
        if (isInCheck(colour, logic)) {
            return false;
        }
        List<Figure> myFigures = logic.getFigures().stream()
                .filter(f -> f.type.startsWith(colour))
                .toList();

        // Jede Figur der eigenen Farbe prüfen
        for (Figure figure : myFigures) {
            if (figure.type.startsWith(colour)) {
                // Alle möglichen Felder prüfen
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        if (figure.canMoveTo(col, row, logic)) {
                            int oldCol = figure.col;
                            int oldRow = figure.row;
                            Figure target = logic.getFigureAt(col, row);

                            figure.col = col;
                            figure.row = row;
                            if (target != null) {
                                target.col = -1;
                                target.row = -1;
                            }

                            boolean stillInCheck = isInCheck(colour, logic);

                            // Zurücksetzen
                            figure.col = oldCol;
                            figure.row = oldRow;
                            if (target != null) {
                                target.col = col;
                                target.row = row;
                            }

                            // Wenn es einen Zug gibt der Spieler nicht ins Schach bringt = KEIN PAT
                            if (!stillInCheck) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // Keine Züge möglich und kein Schach = PATT
        return true;
    }

    public static boolean isSquareAttacked(int col, int row, String victimColour, BoardLogic logic) {
        String enemyColour;
        if (victimColour.equals("w")) {
            enemyColour = "b";
        } else  {
            enemyColour = "w";
        }
        for (Figure figure : logic.getFigures()) {
            if (figure.type.startsWith(enemyColour)) {
                // enemy figure
                if (figure.canMoveTo(col, row, logic)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isInsufficientMaterial(BoardLogic logic) {
        List<Figure> allFigures = logic.getFigures();

        // 2 Könige = Remis
        if (allFigures.size() == 2) {
            return true;
        }
        // König + Springer oder Läufer = Remis
        if (allFigures.size() == 3) {
            for (Figure f : allFigures) {
                if (f instanceof Bishop || f instanceof Knight) return true;
            }
        }
        return false;
    }
}