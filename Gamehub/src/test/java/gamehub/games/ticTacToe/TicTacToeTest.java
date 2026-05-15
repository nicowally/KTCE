package gamehub.games.ticTacToe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TicTacToeTest {

    private TicTacToeGame game;

    @BeforeEach
    public void setup() {
        game = new TicTacToeGame();
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    public void testInitialState() {
        assertEquals(GameState.PLAYING, game.getGameState());
        assertEquals(Player.X, game.getCurrentPlayer());
        assertFalse(game.isGameOver());
        assertNull(game.getWinningLine());
        for (int i = 0; i < 9; i++) {
            assertNull(game.getCell(i), "Cell " + i + " should be empty at start");
        }
    }

    // ── Basic move mechanics ──────────────────────────────────────────────────

    @Test
    public void testValidMove() {
        assertTrue(game.makeMove(0));
        assertEquals(Player.X, game.getCell(0));
    }

    @Test
    public void testPlayerAlternatesAfterMove() {
        game.makeMove(0); // X
        assertEquals(Player.O, game.getCurrentPlayer());
        game.makeMove(1); // O
        assertEquals(Player.X, game.getCurrentPlayer());
    }

    @Test
    public void testOccupiedCellRejected() {
        game.makeMove(4); // X plays center
        assertFalse(game.makeMove(4)); // O tries same cell
        assertEquals(Player.X, game.getCell(4)); // still X
        assertEquals(Player.O, game.getCurrentPlayer()); // turn didn't advance
    }

    @Test
    public void testOutOfBoundsThrows() {
        assertThrows(IllegalArgumentException.class, () -> game.makeMove(-1));
        assertThrows(IllegalArgumentException.class, () -> game.makeMove(9));
    }

    // ── Win detection ─────────────────────────────────────────────────────────

    @Test
    public void testTopRowWin() {
        // X: 0,1,2  O: 3,4
        game.makeMove(0); game.makeMove(3);
        game.makeMove(1); game.makeMove(4);
        game.makeMove(2);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertTrue(game.isGameOver());
        assertArrayEquals(new int[]{0, 1, 2}, game.getWinningLine());
    }

    @Test
    public void testMiddleRowWin() {
        // X: 3,4,5  O: 0,1
        game.makeMove(3); game.makeMove(0);
        game.makeMove(4); game.makeMove(1);
        game.makeMove(5);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{3, 4, 5}, game.getWinningLine());
    }

    @Test
    public void testBottomRowWin() {
        // X: 6,7,8  O: 0,1
        game.makeMove(6); game.makeMove(0);
        game.makeMove(7); game.makeMove(1);
        game.makeMove(8);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{6, 7, 8}, game.getWinningLine());
    }

    @Test
    public void testLeftColumnWin() {
        // X: 0,3,6  O: 1,2
        game.makeMove(0); game.makeMove(1);
        game.makeMove(3); game.makeMove(2);
        game.makeMove(6);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{0, 3, 6}, game.getWinningLine());
    }

    @Test
    public void testMiddleColumnWin() {
        // X: 1,4,7  O: 0,2
        game.makeMove(1); game.makeMove(0);
        game.makeMove(4); game.makeMove(2);
        game.makeMove(7);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{1, 4, 7}, game.getWinningLine());
    }

    @Test
    public void testRightColumnWin() {
        // X: 2,5,8  O: 0,1
        game.makeMove(2); game.makeMove(0);
        game.makeMove(5); game.makeMove(1);
        game.makeMove(8);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{2, 5, 8}, game.getWinningLine());
    }

    @Test
    public void testMainDiagonalWin() {
        // X: 0,4,8  O: 1,2
        game.makeMove(0); game.makeMove(1);
        game.makeMove(4); game.makeMove(2);
        game.makeMove(8);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{0, 4, 8}, game.getWinningLine());
    }

    @Test
    public void testAntiDiagonalWin() {
        // X: 2,4,6  O: 0,1
        game.makeMove(2); game.makeMove(0);
        game.makeMove(4); game.makeMove(1);
        game.makeMove(6);

        assertEquals(GameState.X_WINS, game.getGameState());
        assertArrayEquals(new int[]{2, 4, 6}, game.getWinningLine());
    }

    @Test
    public void testOPlayerWins() {
        // O wins column 0:  X: 1,2  O: 0,3,6
        game.makeMove(1); game.makeMove(0);
        game.makeMove(2); game.makeMove(3);
        game.makeMove(8); game.makeMove(6);

        assertEquals(GameState.O_WINS, game.getGameState());
        assertArrayEquals(new int[]{0, 3, 6}, game.getWinningLine());
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    @Test
    public void testDraw() {
        // Classic draw sequence: X 0,2,5,6,7  O 1,3,4,8
        int[] moves = {0, 1, 2, 3, 5, 4, 6, 8, 7};
        for (int m : moves) game.makeMove(m);

        assertEquals(GameState.DRAW, game.getGameState());
        assertTrue(game.isGameOver());
        assertNull(game.getWinningLine());
    }

    // ── No moves after game over ───────────────────────────────────────────────

    @Test
    public void testNoMoveAfterGameOver() {
        // X wins top row
        game.makeMove(0); game.makeMove(3);
        game.makeMove(1); game.makeMove(4);
        game.makeMove(2);

        assertTrue(game.isGameOver());
        assertFalse(game.makeMove(5)); // rejected
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    public void testReset() {
        game.makeMove(0); game.makeMove(3);
        game.makeMove(1); game.makeMove(4);
        game.makeMove(2); // X wins

        game.reset();

        assertEquals(GameState.PLAYING, game.getGameState());
        assertEquals(Player.X, game.getCurrentPlayer());
        assertFalse(game.isGameOver());
        assertNull(game.getWinningLine());
        for (int i = 0; i < 9; i++) assertNull(game.getCell(i));
    }

    // ── getBoard() defensive copy ─────────────────────────────────────────────

    @Test
    public void testGetBoardReturnsCopy() {
        game.makeMove(0);
        Player[] board = game.getBoard();
        board[0] = null; // mutate copy
        assertEquals(Player.X, game.getCell(0)); // original unchanged
    }
}
