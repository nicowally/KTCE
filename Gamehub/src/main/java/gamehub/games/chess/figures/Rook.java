package gamehub.games.chess.figures;

import gamehub.gamehub.ChessBoard;
import gamehub.games.chess.Figure;

public class Rook extends Figure {

    public Rook(String colour, int col, int row) {
        super(colour + "R", col, row);
    }

    @Override
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard chessBoard) {
        if(targetCol != col && targetRow != row) {
            return false;
        }
        if(!chessBoard.isPathClear(col,row,targetCol,targetRow)) {
            return false;
        }
        Figure target = chessBoard.getFigureAt(targetCol, targetRow);
        if (target != null && target.type.startsWith(this.type.substring(0,1))) {
            return false;
        }
    return  true;
    }
}
