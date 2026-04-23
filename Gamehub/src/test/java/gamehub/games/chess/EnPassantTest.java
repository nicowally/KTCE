package gamehub.games.chess;

import gamehub.games.chess.BoardLogic;
import gamehub.games.chess.figures.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnPassantTest {
    private BoardLogic logic;

    @BeforeEach
    public void setup() {
        logic = new BoardLogic();
    }

    @Test
    public void testEnPassantCapture() {
        Pawn whitePawn = new Pawn("w", 4, 3);
        Pawn blackPawn = new Pawn("b", 5, 1);
        logic.addFigure(whitePawn);
        logic.addFigure(blackPawn);

        blackPawn.row = 3;
        logic.setEnPassantTarget(blackPawn);

        assertTrue(whitePawn.canMoveTo(5, 2, logic));

        logic.setEnPassantTarget(null);
        assertNull(logic.getEnPassantTarget());
    }
    @Test
    public void testEnPassantExpires() {
        BoardLogic logic = new BoardLogic();

        Pawn whitePawn = new Pawn("w", 4, 3);
        Pawn blackPawn = new Pawn("b", 5, 1);
        logic.addFigure(whitePawn);
        logic.addFigure(blackPawn);

        blackPawn.row = 3;
        logic.setEnPassantTarget(blackPawn);

        assertTrue(whitePawn.canMoveTo(5, 2, logic));

        // Andere zug passiert - daher setzen wir setzen das Target zurück, weil der Zug vorbei ist
        logic.setEnPassantTarget(null);

        assertFalse(whitePawn.canMoveTo(5, 2, logic));
    }
}
