package gamehub.games.ticTacToe;


public class TicTacToeGame {

    // Winning combinations as board indices (0-8, row-major)
    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
            {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    private Player[] board;
    private Player currentPlayer;
    private GameState gameState;
    private int[] winningLine;

    public TicTacToeGame() {
        reset();
    }

    /** Resets the board for a new game. X always goes first. */
    public void reset() {
        board = new Player[9];
        currentPlayer = Player.X;
        gameState = GameState.PLAYING;
        winningLine = null;
    }

    /**
     * Attempts to place the current player's mark at the given cell index.
     *
     * @param index Cell index (0–8, row-major)
     * @return true if the move was accepted, false if the cell is already
     *         occupied or the game has ended
     */
    public boolean makeMove(int index) {
        if (index < 0 || index > 8) throw new IllegalArgumentException("Index must be 0–8");
        if (gameState != GameState.PLAYING || board[index] != null) return false;

        board[index] = currentPlayer;

        int[] line = findWinningLine();
        if (line != null) {
            winningLine = line;
            gameState = (currentPlayer == Player.X) ? GameState.X_WINS : GameState.O_WINS;
        } else if (isBoardFull()) {
            gameState = GameState.DRAW;
        } else {
            currentPlayer = (currentPlayer == Player.X) ? Player.O : Player.X;
        }

        return true;
    }

    /** Returns the player occupying the cell, or null if empty. */
    public Player getCell(int index) {
        return board[index];
    }

    public Player[] getBoard() {
        return board.clone();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    /** Returns the 3 winning cell indices, or null if there is no winner yet. */
    public int[] getWinningLine() {
        return winningLine == null ? null : winningLine.clone();
    }

    public boolean isGameOver() {
        return gameState != GameState.PLAYING;
    }

    private int[] findWinningLine() {
        for (int[] line : WINNING_LINES) {
            Player a = board[line[0]];
            if (a != null && a == board[line[1]] && a == board[line[2]]) {
                return line;
            }
        }
        return null;
    }

    private boolean isBoardFull() {
        for (Player cell : board) {
            if (cell == null) return false;
        }
        return true;
    }
}