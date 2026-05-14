package gamehub.games.ticTacToe;

public class TicTacToeAI {

    /** Returns the best move index (0–8) for the given board state. */
    public static int getBestMove(Player[] board, Player aiPlayer) {
        Player human = (aiPlayer == Player.X) ? Player.O : Player.X;
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = aiPlayer;
                int score = minimax(board, false, aiPlayer, human);
                board[i] = null;
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }
            }
        }
        return bestMove;
    }

    private static int minimax(Player[] board, boolean isMaximizing,
                               Player aiPlayer, Player human) {
        Player winner = checkWinner(board);
        if (winner == aiPlayer)  return 10;
        if (winner == human)     return -10;
        if (isFull(board))       return 0;

        int best = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = isMaximizing ? aiPlayer : human;
                int score = minimax(board, !isMaximizing, aiPlayer, human);
                board[i] = null;
                best = isMaximizing ? Math.max(best, score) : Math.min(best, score);
            }
        }
        return best;
    }

    private static Player checkWinner(Player[] board) {
        int[][] lines = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (int[] line : lines) {
            Player a = board[line[0]];
            if (a != null && a == board[line[1]] && a == board[line[2]]) return a;
        }
        return null;
    }

    private static boolean isFull(Player[] board) {
        for (Player p : board) if (p == null) return false;
        return true;
    }
}