package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.Check;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Knight;
import gamehub.games.chess.figures.Queen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RemiseTest {

    @Test
    public void testStalemate() {
        BoardLogic logic = new BoardLogic();

        // König in der Ecke (0,0)
        logic.addFigure(new King("b", 0, 0));

        // Dame blockiert Fluchtwege auf (1,2) und (2,1)
        logic.addFigure(new Queen("w", 1, 2));

        //Weißer König für die Regel-Logik
        logic.addFigure(new King("w", 7, 7));

        // Patt-Bedingung: Schwarz steht NICHT im Schach und Schwarz hat keine legalen Züge mehr
        assertFalse(Check.isInCheck("b", logic));
        assertTrue(Check.isStalemate("b", logic));
    }

    @Test
    public void testInsufficientMaterialRemis() {
        BoardLogic logic = new BoardLogic();

        // 2 Könige = Remis
        logic.addFigure(new King("w", 0, 0));
        logic.addFigure(new King("b", 7, 7));

        assertTrue(Check.isInsufficientMaterial(logic), "Zwei Könige alleine sind immer ein Remis");
    }
    @Test
    public void testInsufficientMaterialWithKnight() {
        BoardLogic logic = new BoardLogic();
        logic.addFigure(new King("w", 0, 0));
        logic.addFigure(new King("b", 7, 7));
        logic.addFigure(new Knight("w", 1, 1));

        assertTrue(Check.isInsufficientMaterial(logic));
    }
}