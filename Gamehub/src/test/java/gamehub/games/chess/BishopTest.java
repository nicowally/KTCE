package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Bishop;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BishopTest {

    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testDiagonalMovement() {

        Bishop bishop = new Bishop("w", 3, 4);
        logic.addFigure(bishop);

        assertTrue(bishop.canMoveTo(5, 6, logic));
        assertTrue(bishop.canMoveTo(1, 2, logic));
        assertTrue(bishop.canMoveTo(5, 2, logic));
        assertTrue(bishop.canMoveTo(1, 6, logic));

        assertFalse(bishop.canMoveTo(3, 2, logic));
        assertFalse(bishop.canMoveTo(5, 4, logic));
    }

    @Test
    public void testBlockedByOwnFigure() {
        Bishop bishop = new Bishop("w", 3, 4);
        Pawn friendlyPawn = new Pawn("w", 5, 6);

        logic.addFigure(bishop);
        logic.addFigure(friendlyPawn);

        assertTrue(bishop.canMoveTo(4, 5, logic));
        assertFalse(bishop.canMoveTo(5, 6, logic));
        assertFalse(bishop.canMoveTo(6, 7, logic));
    }

    @Test
    public void testCaptureEnemyFigure() {
        Bishop bishop = new Bishop("w", 3, 4);
        Pawn enemyPawn = new Pawn("b", 5, 6);

        logic.addFigure(bishop);
        logic.addFigure(enemyPawn);

        assertTrue(bishop.canMoveTo(5, 6, logic));
        assertFalse(bishop.canMoveTo(6, 7, logic));
    }
}
