package gamehub.games.chess.figures;

import gamehub.games.chess.Board;
import gamehub.games.chess.Figure;

public class Rook extends Figure {

    public Rook(String colour, int col, int row) {
        super(colour + "R", col, row);
    }

    @Override
    public boolean canMoveTo(int targetCol, int targetRow, Board board) {
        if(targetCol != col && targetRow != row) {
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
