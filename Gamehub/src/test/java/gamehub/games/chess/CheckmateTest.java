package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.Check;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Queen;
import gamehub.games.chess.figures.Rook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckmateTest {

    @Test
    public void testCheckmate() {
        BoardLogic logic = new BoardLogic();

        // Schwarzer König in der Ecke (0,0)
        logic.addFigure(new King("b", 0, 0));
        // Weiße Dame gibt Schach auf (0,1)
        logic.addFigure(new Queen("w", 0, 1));
        // Weißer Turm deckt die Dame auf der Linie 0
        logic.addFigure(new Rook("w", 7, 1));

        // Weißer König irgendwo auf dem Brett
        logic.addFigure(new King("w", 7, 7));

        assertTrue(Check.isInCheck("b", logic));
        assertTrue(Check.isCheckmate("b", logic));
    }

    @Test
    public void testEscapePossible() {
        BoardLogic logic = new BoardLogic();
        // König im Schach,kann aber auf ein freies Feld ausweichen
        logic.addFigure(new King("b", 0, 0));
        logic.addFigure(new Queen("w", 0, 5));
        logic.addFigure(new King("w", 7, 7));

        assertTrue(Check.isInCheck("b", logic));
        assertFalse(Check.isCheckmate("b", logic));
    }
}