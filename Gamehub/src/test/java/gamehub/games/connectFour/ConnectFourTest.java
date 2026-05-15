package gamehub.games.connectFour;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConnectFourTest {

    private Connect4Game game;

    @BeforeEach
    public void setup() {
        game = new Connect4Game();
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    public void testInitialState() {
        assertEquals(Connect4GameState.PLAYING, game.getGameState());
        assertEquals(Connect4Player.RED, game.getCurrentPlayer());
        assertFalse(game.isGameOver());
        assertNull(game.getWinningCells());
        for (int r = 0; r < Connect4Game.ROWS; r++)
            for (int c = 0; c < Connect4Game.COLS; c++)
                assertNull(game.getCell(r, c), "Cell [" + r + "][" + c + "] should start empty");
    }

    // ── Basic drop mechanics ──────────────────────────────────────────────────

    @Test
    public void testDiscFallsToBottomRow() {
        int row = game.dropDisc(0);
        assertEquals(Connect4Game.ROWS - 1, row);
        assertEquals(Connect4Player.RED, game.getCell(Connect4Game.ROWS - 1, 0));
    }

    @Test
    public void testDiscsStack() {
        game.dropDisc(3); // RED  -> row 5
        int row = game.dropDisc(3); // YELLOW -> row 4
        assertEquals(Connect4Game.ROWS - 2, row);
        assertEquals(Connect4Player.YELLOW, game.getCell(Connect4Game.ROWS - 2, 3));
    }

    @Test
    public void testPlayerAlternates() {
        assertEquals(Connect4Player.RED, game.getCurrentPlayer());
        game.dropDisc(0);
        assertEquals(Connect4Player.YELLOW, game.getCurrentPlayer());
        game.dropDisc(1);
        assertEquals(Connect4Player.RED, game.getCurrentPlayer());
    }

    @Test
    public void testFullColumnRejected() {
        for (int i = 0; i < Connect4Game.ROWS; i++) game.dropDisc(0);
        assertFalse(game.isColumnPlayable(0));
        assertEquals(-1, game.dropDisc(0));
    }

    @Test
    public void testInvalidColumnThrows() {
        assertThrows(IllegalArgumentException.class, () -> game.dropDisc(-1));
        assertThrows(IllegalArgumentException.class, () -> game.dropDisc(Connect4Game.COLS));
    }

    // ── isColumnPlayable ──────────────────────────────────────────────────────

    @Test
    public void testColumnPlayableAfterPartialFill() {
        game.dropDisc(2);
        assertTrue(game.isColumnPlayable(2));
    }

    @Test
    public void testColumnNotPlayableWhenFull() {
        for (int i = 0; i < Connect4Game.ROWS; i++) game.dropDisc(0);
        assertFalse(game.isColumnPlayable(0));
    }

    // ── Horizontal win ────────────────────────────────────────────────────────

    @Test
    public void testHorizontalWinRed() {
        // RED wins cols 0-3, YELLOW uses col 6 as filler
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(1); game.dropDisc(6);
        game.dropDisc(2); game.dropDisc(6);
        game.dropDisc(3);

        assertEquals(Connect4GameState.RED_WINS, game.getGameState());
        assertTrue(game.isGameOver());
        assertNotNull(game.getWinningCells());
        assertEquals(4, game.getWinningCells().length);
    }

    @Test
    public void testHorizontalWinYellow() {
        // YELLOW wins cols 3-6; RED uses col 0-1 as filler
        game.dropDisc(0); game.dropDisc(3);
        game.dropDisc(0); game.dropDisc(4);
        game.dropDisc(0); game.dropDisc(5);
        game.dropDisc(1); game.dropDisc(6);

        assertEquals(Connect4GameState.YELLOW_WINS, game.getGameState());
        assertNotNull(game.getWinningCells());
    }

    // ── Vertical win ──────────────────────────────────────────────────────────

    @Test
    public void testVerticalWinRed() {
        // RED stacks col 0 four times;
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0);

        assertEquals(Connect4GameState.RED_WINS, game.getGameState());
        assertNotNull(game.getWinningCells());
        assertEquals(4, game.getWinningCells().length);
    }

    @Test
    public void testVerticalWinYellow() {
        // YELLOW stacks col 1 four times; RED uses cols 0 and 6 as fillers
        game.dropDisc(0); game.dropDisc(1);
        game.dropDisc(0); game.dropDisc(1);
        game.dropDisc(0); game.dropDisc(1);
        game.dropDisc(6); game.dropDisc(1);

        assertEquals(Connect4GameState.YELLOW_WINS, game.getGameState());
    }

    // ── Diagonal win ──────────────────────────────────────────────────────────

    /**
     * Descending diagonal (\) for YELLOW at (5,3), (4,2), (3,1), (2,0).
     *
     * To land YELLOW at the correct rows we pad each column with the right
     * number of discs before placing the winning one:
     *   col 3: 0 pads  -> YELLOW lands at row 5
     *   col 2: 1 pad   -> YELLOW lands at row 4
     *   col 1: 2 pads  -> YELLOW lands at row 3
     *   col 0: 3 pads  -> YELLOW lands at row 2
     */
    @Test
    public void testDiagonalDescendingWinYellow() {
        game.dropDisc(6); // RED   filler (RED must move first)
        game.dropDisc(3); // YELLOW (5,3) checkmark 1

        game.dropDisc(2); // RED   (5,2) pad
        game.dropDisc(2); // YELLOW (4,2) checkmark 2

        game.dropDisc(1); // RED   (5,1) pad
        game.dropDisc(1); // YELLOW (4,1) pad
        game.dropDisc(6); // RED   filler
        game.dropDisc(1); // YELLOW (3,1) checkmark 3

        game.dropDisc(0); // RED   (5,0) pad
        game.dropDisc(0); // YELLOW (4,0) pad
        game.dropDisc(0); // RED   (3,0) pad
        game.dropDisc(0); // YELLOW (2,0) checkmark 4 -> YELLOW wins

        assertEquals(Connect4GameState.YELLOW_WINS, game.getGameState());
        assertNotNull(game.getWinningCells());
        assertEquals(4, game.getWinningCells().length);
    }

    /**
     * Ascending diagonal (/) for RED at (5,0), (4,1), (3,2), (2,3).
     *
     * Padding per column so RED lands at the target row:
     *   col 0: 0 pads  -> RED at row 5  (RED goes first naturally)
     *   col 1: 1 pad   -> RED at row 4
     *   col 2: 2 pads  -> RED at row 3
     *   col 3: 3 pads  -> RED at row 2
     */
    @Test
    public void testDiagonalAscendingWinRed() {
        game.dropDisc(0); // RED   (5,0) checkmark 1

        game.dropDisc(1); // YELLOW (5,1) pad
        game.dropDisc(1); // RED   (4,1) checkmark 2

        game.dropDisc(2); // YELLOW (5,2) pad
        game.dropDisc(2); // RED   (4,2) pad
        game.dropDisc(6); // YELLOW filler (keeps turn on RED)
        game.dropDisc(2); // RED   (3,2) checkmark 3

        game.dropDisc(3); // YELLOW (5,3) pad
        game.dropDisc(3); // RED   (4,3) pad
        game.dropDisc(3); // YELLOW (3,3) pad
        game.dropDisc(3); // RED   (2,3) checkmark 4 -> RED wins

        assertEquals(Connect4GameState.RED_WINS, game.getGameState());
        assertNotNull(game.getWinningCells());
        assertEquals(4, game.getWinningCells().length);
    }

    // ── No moves after game over ──────────────────────────────────────────────

    @Test
    public void testNoDropAfterGameOver() {
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); // RED wins

        assertTrue(game.isGameOver());
        assertEquals(-1, game.dropDisc(1));
    }

    // ── Winning cells defensive copy ─────────────────────────────────────────

    @Test
    public void testWinningCellsReturnsCopy() {
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0);

        int[] cells = game.getWinningCells();
        cells[0] = -999;
        assertNotEquals(-999, game.getWinningCells()[0]);
    }

    @Test
    public void testWinningCellsNullBeforeWin() {
        game.dropDisc(0);
        assertNull(game.getWinningCells());
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /**
     * Fills the entire board via round-robin column drops and asserts the game
     * is terminal (not PLAYING).  Connect-4 draw positions are hard to construct
     * by hand without a solver; we accept either DRAW or a WIN — both are valid
     * terminal states — and verify the winning-cells contract is respected.
     */
    @Test
    public void testBoardFullIsTerminal() {
        for (int round = 0; round < Connect4Game.ROWS; round++) {
            for (int col = 0; col < Connect4Game.COLS; col++) {
                if (game.isGameOver()) break;
                game.dropDisc(col);
            }
            if (game.isGameOver()) break;
        }
        assertNotEquals(Connect4GameState.PLAYING, game.getGameState());
        if (game.getGameState() == Connect4GameState.DRAW) {
            assertNull(game.getWinningCells());
        } else {
            assertNotNull(game.getWinningCells());
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    public void testReset() {
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); game.dropDisc(6);
        game.dropDisc(0); // RED wins

        game.reset();

        assertEquals(Connect4GameState.PLAYING, game.getGameState());
        assertEquals(Connect4Player.RED, game.getCurrentPlayer());
        assertFalse(game.isGameOver());
        assertNull(game.getWinningCells());
        for (int r = 0; r < Connect4Game.ROWS; r++)
            for (int c = 0; c < Connect4Game.COLS; c++)
                assertNull(game.getCell(r, c));
    }
}
