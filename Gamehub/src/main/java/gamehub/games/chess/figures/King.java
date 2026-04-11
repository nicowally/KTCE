package gamehub.games.chess.figures;

import gamehub.gamehub.ChessBoard;
import gamehub.games.chess.Figure;

public class King extends Figure {
    public King(String colour, int col, int row) {
        super(colour + "K", col, row);
    }
    @Override
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard chessBoard) {
        int diffCol = Math.abs(targetCol - col);
        int diffRow = Math.abs(targetRow - row);

        if(Math.max(diffCol,diffRow) !=1) return false;

        Figure target = chessBoard.getFigureAt(targetCol, targetRow);
        if (target != null && target.type.startsWith(this.type.substring(0,1))) {
            return false;
        }
        return true;
    }
}
