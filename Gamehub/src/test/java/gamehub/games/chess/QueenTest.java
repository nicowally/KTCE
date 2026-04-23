package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Queen;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueenTest {

    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testMovementAllDirections() {
        Queen queen = new Queen("w", 3, 3);
        logic.addFigure(queen);

        assertTrue(queen.canMoveTo(3, 0, logic));
        assertTrue(queen.canMoveTo(7, 3, logic));
        assertTrue(queen.canMoveTo(0, 0, logic));
        assertTrue(queen.canMoveTo(6, 6, logic));

        assertFalse(queen.canMoveTo(4, 5, logic));
    }

    @Test
    public void testBlockedPath() {
        Queen queen = new Queen("w", 3, 3);
        Pawn blockingPawn = new Pawn("w", 3, 5);
        logic.addFigure(queen);
        logic.addFigure(blockingPawn);

        assertTrue(queen.canMoveTo(3, 4, logic));
        assertFalse(queen.canMoveTo(3, 5, logic));
        assertFalse(queen.canMoveTo(3, 6, logic));
    }
    @Test
    public void testCaptureEnemyAndStop() {
        Queen queen = new Queen("w", 3, 3);
        Pawn enemy = new Pawn("b", 3, 5);

        logic.addFigure(queen);
        logic.addFigure(enemy);

        assertTrue(queen.canMoveTo(3, 5, logic));
        assertFalse(queen.canMoveTo(3, 6, logic));
    }
}