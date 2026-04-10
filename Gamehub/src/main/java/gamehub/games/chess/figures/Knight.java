package gamehub.games.chess.figures;

import gamehub.games.chess.ChessBoard;
import gamehub.games.chess.Figure;

public class Knight extends Figure {

    public Knight(String colour, int col, int row) {
        super(colour + "N", col, row);
    }
    @Override
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard chessBoard) {
        int diffCol = Math.abs(targetCol - col);
        int diffRow = Math.abs(targetRow - row);
        //  LMove weil immer 2 in eine Richtung, 1 in die andere
        boolean isLMove = (diffCol == 2 && diffRow == 1) || (diffCol == 1 && diffRow == 2);

        if (!isLMove) return false;

        // Prüfen, ob auf dem Zielfeld eine eigene Figur steht
        Figure target = chessBoard.getFigureAt(targetCol, targetRow);
        if (target != null && target.type.startsWith(this.type.substring(0,1))) {
            return false;
        }
        return true;
    }
}
