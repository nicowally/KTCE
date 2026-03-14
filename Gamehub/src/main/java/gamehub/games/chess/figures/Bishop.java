package gamehub.games.chess.figures;

import gamehub.games.chess.Board;
import gamehub.games.chess.Figure;

public class Bishop extends Figure {

    public Bishop(String colour, int col, int row) {
        super(colour + "B", col, row);
    }

    @Override
    public boolean canMoveTo(int targetCol, int targetRow, Board board) {
        int diffCol = Math.abs(targetCol - col);
        int diffRow = Math.abs(targetRow - row);

        // diagonal = Abstand Spalte == Abstand Reihe
        if (diffCol != diffRow) {
            return false;
        }
        if(!board.isPathClear(col,row,targetCol,targetRow)) {
            return false;
        }
        Figure target = board.getFigureAt(targetCol, targetRow);
        if (target != null && target.type.startsWith(this.type.substring(0,1))) {
            return false;
        }
        return  true;
    }
}
