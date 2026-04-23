package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PawnPromotionTest {
    private BoardLogic logic;

    @BeforeEach
    public void setup() { logic = new BoardLogic(); }

    @Test
    public void testPromotionRow() {
        Pawn whitePawn = new Pawn("w", 0, 1);
        logic.addFigure(whitePawn);

        // Bauer zieht auf Reihe 0
        whitePawn.row = 0;
        assertTrue(whitePawn.isPromotionRow(0));
        assertFalse(whitePawn.isPromotionRow(1));
    }
}