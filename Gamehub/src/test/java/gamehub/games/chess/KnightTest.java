package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Knight;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KnightTest {

    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testLMovement() {
        Knight knight = new Knight("w", 4, 4);
        logic.addFigure(knight);

        // Alle 8 L-Positionen
        assertTrue(knight.canMoveTo(5, 2, logic));
        assertTrue(knight.canMoveTo(3, 2, logic));
        assertTrue(knight.canMoveTo(6, 3, logic));
        assertTrue(knight.canMoveTo(2, 3, logic));
        assertTrue(knight.canMoveTo(6, 5, logic));
        assertTrue(knight.canMoveTo(2, 5, logic));
        assertTrue(knight.canMoveTo(5, 6, logic));
        assertTrue(knight.canMoveTo(3, 6, logic));

        // Ungültige Züge
        assertFalse(knight.canMoveTo(4, 5, logic));
        assertFalse(knight.canMoveTo(6, 6, logic));
    }

    @Test
    public void testJumpingOverFigures() {
        Knight knight = new Knight("w", 4, 4);
        // Bauern umzingeln den Springer
        logic.addFigure(knight);
        logic.addFigure(new Pawn("w", 4, 3));
        logic.addFigure(new Pawn("b", 3, 3));
        logic.addFigure(new Pawn("w", 5, 4));

        // Springer kann drüber springen
        assertTrue(knight.canMoveTo(5, 2, logic));
    }
    @Test
    public void testCaptureEnemy() {
        Knight knight = new Knight("w", 4, 4);
        Pawn enemy = new Pawn("b", 5, 2);
        Pawn friendly = new Pawn("w", 3, 2);

        logic.addFigure(knight);
        logic.addFigure(enemy);
        logic.addFigure(friendly);

        assertTrue(knight.canMoveTo(5, 2, logic));
        assertFalse(knight.canMoveTo(3, 2, logic));
    }
}