package gamehub.games.chess.figures;

import gamehub.gamehub.ChessBoard;
import gamehub.games.chess.Figure;

public class Pawn extends Figure {

    public Pawn(String colour, int col, int row) {
        super(colour + "P", col, row);
    }

    @Override
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard chessBoard) {
        int direction;
        int startRow;

        if (type.startsWith("w")) {
            direction = -1; // weiß bewegt sich nach oben
            startRow = 6;
        } else {
            direction = 1;  // schwarz bewegt sich nach unten
            startRow = 1;
        }
        // 1 Feld nach vorne
        if(targetCol == col && targetRow == row+direction) {
            if(chessBoard.getFigureAt(targetCol, targetRow) == null){
                return true;
            }
        }
        // Am Anfang 2 Felder nach vorne
        if (targetCol == col && targetRow == row + 2*direction && row == startRow) {
            if (chessBoard.getFigureAt(targetCol, targetRow) == null && chessBoard.getFigureAt(targetCol, row + direction) == null) {
                return true;
            }
        }

        // Schlagen diagonal
        if (Math.abs(targetCol - col) == 1 && targetRow == row + direction) {
            Figure target = chessBoard.getFigureAt(targetCol, targetRow);
            if (target != null && !target.type.startsWith(type.substring(0, 1))) {
                return true;
            }
        }

        return false;
    }
}
