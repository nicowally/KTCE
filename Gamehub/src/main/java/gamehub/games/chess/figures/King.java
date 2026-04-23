package gamehub.games.chess.figures;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.Check;
import gamehub.games.chess.Figure;

public class King extends Figure {
    public King(String colour, int col, int row) {
        super(colour + "K", col, row);
    }
    @Override
    public boolean canMoveTo(int targetCol, int targetRow, BoardLogic logic) {
        int diffCol = Math.abs(targetCol - col);
        int diffRow = Math.abs(targetRow - row);
        String myColour = this.type.substring(0,1);

        if(Math.max(diffCol,diffRow) ==1) {
            Figure target = logic.getFigureAt(targetCol, targetRow);
            if (target != null && target.type.startsWith(myColour)) {
                return false;
            }
            return true;
        }
        if(!this.hasMoved && diffRow == 0 && diffCol == 2){
            if(Check.isInCheck(myColour,logic)) {
                return false;
            }
            int rookCol;
            if(targetCol > col) {
                rookCol = 7;
            } else {
                rookCol = 0;
            }
            Figure rook = logic.getFigureAt(rookCol,row);

            if(rook instanceof Rook && !rook.hasMoved) {
                if(logic.isPathClear(this.col,this.row,rookCol,row)) {
                    int step;
                    if (targetCol > col) {
                        step = 1;
                    } else {
                        step = -1;
                    }
                    if(Check.isSquareAttacked(this.col + step,this.row, myColour,logic)) {
                        return false;
                    }
                    if(Check.isSquareAttacked(this.col +2 *step,this.row,myColour,logic)){
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
