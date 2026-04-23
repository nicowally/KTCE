package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Rook;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RookTest {
    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testMovement() {
        Rook rook = new Rook("w", 3, 3);
        logic.addFigure(rook);

        assertTrue(rook.canMoveTo(3, 0, logic));
        assertTrue(rook.canMoveTo(7, 3, logic));
        assertFalse(rook.canMoveTo(4, 4, logic));
    }

    @Test
    public void testCapture() {
        Rook rook = new Rook("w", 3, 3);
        Pawn enemy = new Pawn("b", 3, 6);
        logic.addFigure(rook);
        logic.addFigure(enemy);

        assertTrue(rook.canMoveTo(3, 6, logic));
        assertFalse(rook.canMoveTo(3, 7, logic));
    }
}