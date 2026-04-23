package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.King;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KingTest {

    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testBasicMovement() {
        King king = new King("w", 4, 4);
        logic.addFigure(king);

        assertTrue(king.canMoveTo(4, 3, logic));
        assertTrue(king.canMoveTo(4, 5, logic));
        assertTrue(king.canMoveTo(3, 4, logic));
        assertTrue(king.canMoveTo(5, 4, logic));
        assertTrue(king.canMoveTo(3, 3, logic));
        assertTrue(king.canMoveTo(5, 5, logic));

        assertFalse(king.canMoveTo(4, 2, logic));
        assertFalse(king.canMoveTo(6, 4, logic));
        assertFalse(king.canMoveTo(6, 6, logic));
    }

    @Test
    public void testBlockedByOwnFigure() {
        King king = new King("w", 4, 4);
        Pawn friendlyPawn = new Pawn("w", 4, 3);
        logic.addFigure(king);
        logic.addFigure(friendlyPawn);

        assertFalse(king.canMoveTo(4, 3, logic));
        assertTrue(king.canMoveTo(4, 5, logic));
    }

    @Test
    public void testCaptureEnemy() {
        King king = new King("w", 4, 4);
        Pawn enemyPawn = new Pawn("b", 4, 3);
        logic.addFigure(king);
        logic.addFigure(enemyPawn);

        assertTrue(king.canMoveTo(4, 3, logic));
    }
}