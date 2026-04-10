package gamehub.games.connectFour;

public class Connect4Game {

    public static final int ROWS = 6;
    public static final int COLS = 7;

    private Connect4Player[][] board;
    private Connect4Player currentPlayer;
    private Connect4GameState gameState;
    private int[] winningCells; // flat indices of the 4 winning cells

    public Connect4Game() {
        reset();
    }

    public void reset() {
        board = new Connect4Player[ROWS][COLS];
        currentPlayer = Connect4Player.RED;
        gameState = Connect4GameState.PLAYING;
        winningCells = null;
    }

    /**
     * Drops a disc into the given column.
     *
     * @param col Column index (0–6)
     * @return The row the disc landed in, or -1 if the move was rejected
     */
    public int dropDisc(int col) {
        if (col < 0 || col >= COLS) throw new IllegalArgumentException("Column must be 0–6");
        if (gameState != Connect4GameState.PLAYING) return -1;

        // Find the lowest empty row in this column
        int landingRow = -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == null) {
                landingRow = r;
                break;
            }
        }
        if (landingRow == -1) return -1; // column full

        board[landingRow][col] = currentPlayer;

        int[] line = findWinningCells(landingRow, col);
        if (line != null) {
            winningCells = line;
            gameState = (currentPlayer == Connect4Player.RED)
                    ? Connect4GameState.RED_WINS
                    : Connect4GameState.YELLOW_WINS;
        } else if (isBoardFull()) {
            gameState = Connect4GameState.DRAW;
        } else {
            currentPlayer = (currentPlayer == Connect4Player.RED)
                    ? Connect4Player.YELLOW
                    : Connect4Player.RED;
        }

        return landingRow;
    }

    public Connect4Player getCell(int row, int col) {
        return board[row][col];
    }

    public Connect4Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Connect4GameState getGameState() {
        return gameState;
    }

    public boolean isGameOver() {
        return gameState != Connect4GameState.PLAYING;
    }

    /** Returns flat indices (row * COLS + col) of the 4 winning cells, or null. */
    public int[] getWinningCells() {
        return winningCells == null ? null : winningCells.clone();
    }

    /** Returns true if the given column still has at least one empty cell. */
    public boolean isColumnPlayable(int col) {
        return board[0][col] == null;
    }

    // -------------------------------------------------------------------------

    private int[] findWinningCells(int row, int col) {
        Connect4Player p = board[row][col];
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int[] cells = collectLine(p, row, col, dir[0], dir[1]);
            if (cells != null) return cells;
        }
        return null;
    }

    private int[] collectLine(Connect4Player p, int row, int col, int dr, int dc) {
        // collect up to 4 in both directions along this axis
        java.util.List<int[]> coords = new java.util.ArrayList<>();
        coords.add(new int[]{row, col});

        for (int sign : new int[]{1, -1}) {
            int r = row + sign * dr;
            int c = col + sign * dc;
            while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == p) {
                coords.add(new int[]{r, c});
                r += sign * dr;
                c += sign * dc;
            }
        }

        if (coords.size() < 4) return null;

        // Return flat indices of first 4
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            result[i] = coords.get(i)[0] * COLS + coords.get(i)[1];
        }
        return result;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] == null) return false;
        }
        return true;
    }
}