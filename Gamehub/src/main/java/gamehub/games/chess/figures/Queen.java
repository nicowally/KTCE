package gamehub.games.chess.figures;

import gamehub.games.chess.ChessBoard;
import gamehub.games.chess.Figure;

public class Queen extends Figure {

    public Queen(String colour, int col, int row) {
        super(colour + "Q", col, row);
    }

    @Override
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard chessBoard) {
        int diffCol = Math.abs(targetCol - col);
        int diffRow = Math.abs(targetRow - row);

        boolean moveLikeRook = (targetCol == col || targetRow == row);
        boolean moveLikeBishop = (diffCol == diffRow);

        if(!moveLikeBishop && !moveLikeRook) return false;

        if(!chessBoard.isPathClear(col,row,targetCol,targetRow)) return false;

        Figure target = chessBoard.getFigureAt(targetCol, targetRow);
        if (target != null && target.type.startsWith(this.type.substring(0,1))) {
            return false;
        }
        return true;
    }
}
