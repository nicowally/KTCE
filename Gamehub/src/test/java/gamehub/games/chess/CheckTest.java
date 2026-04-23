package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.Check;
import gamehub.games.chess.figures.Bishop;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Knight;
import gamehub.games.chess.figures.Rook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckTest {

    @Test
    public void testAllFiguresCheck() {
        BoardLogic logic = new BoardLogic();
        King whiteKing = new King("w", 4, 7);
        logic.addFigure(whiteKing);

        // Teste Turm (Vertikal/Horizontal)
        logic.addFigure(new Rook("b", 4, 0));
        assertTrue(Check.isInCheck("w", logic));
        logic.removeFigure(logic.getFigureAt(4, 0)); // Turm wieder weg

        // Teste Läufer (Diagonal)
        logic.addFigure(new Bishop("b", 1, 4));
        assertTrue(Check.isInCheck("w", logic));
        logic.removeFigure(logic.getFigureAt(1, 4));

        // Teste Springer (L-Form)
        logic.addFigure(new Knight("b", 3, 5));
        assertTrue(Check.isInCheck("w", logic));
    }
}