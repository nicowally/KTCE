package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CastlingTest {

    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testValidCastling() {
        King king = new King("w", 4, 7);
        Rook rook = new Rook("w", 7, 7);
        logic.addFigure(king);
        logic.addFigure(rook);

        assertTrue(king.canMoveTo(6, 7, logic));
    }

    @Test
    public void testCastlingWhileInCheck() {
        King king = new King("w", 4, 7);
        Rook rook = new Rook("w", 7, 7);
        Rook enemyRook = new Rook("b", 4, 0); // Gibt Schach
        logic.addFigure(king);
        logic.addFigure(rook);
        logic.addFigure(enemyRook);

        assertFalse(king.canMoveTo(6, 7, logic));
    }

    @Test
    public void testCastlingThroughCheck() {
        King king = new King("w", 4, 7);
        Rook rook = new Rook("w", 7, 7);
        Rook enemyRook = new Rook("b", 5, 0); //Bedroht die Felder
        logic.addFigure(king);
        logic.addFigure(rook);
        logic.addFigure(enemyRook);

        assertFalse(king.canMoveTo(6, 7, logic));
    }
}