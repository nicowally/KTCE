package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PawnTest {
    private BoardLogic logic;

    @BeforeEach
    public void setup() { logic = new BoardLogic(); }

    @Test
    public void testBasicMovement() {
        Pawn whitePawn = new Pawn("w", 3, 6);
        logic.addFigure(whitePawn);

        assertTrue(whitePawn.canMoveTo(3, 5, logic)); // 1 Schritt
        assertTrue(whitePawn.canMoveTo(3, 4, logic)); // 2 Schritte am Start
    }

    @Test
    public void testCaptureOnlyDiagonal() {
        Pawn whitePawn = new Pawn("w", 3, 3);
        Pawn enemy = new Pawn("b", 4, 2);
        Pawn enemyInFront = new Pawn("b", 3, 2);

        logic.addFigure(whitePawn);
        logic.addFigure(enemy);
        logic.addFigure(enemyInFront);

        assertTrue(whitePawn.canMoveTo(4, 2, logic));
        assertFalse(whitePawn.canMoveTo(3, 2, logic));
    }
}