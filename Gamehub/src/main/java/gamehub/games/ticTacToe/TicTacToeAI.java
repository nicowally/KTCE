package gamehub.games.ticTacToe;

import java.util.*;

/**
 * Minimax-based AI for Tic-Tac-Toe with adjustable difficulty.

 * Difficulty (0–100):
 *   0   → fully random (Sehr leicht)
 *   100 → always picks the optimal minimax move (Unschlagbar)

 * In between, the AI picks the optimal move with probability = difficulty/100,
 * and a random legal move otherwise.
 */

public class TicTacToeAI {

    private static final Random RNG = new Random();

    /**
     * Returns the best move index (0–8) for the AI, respecting the difficulty level.
     *
     * @param board      current board state (null = empty cell)
     * @param aiPlayer   the player the AI is controlling
     * @param difficulty 0 (random) to 100 (perfect)
     * @return cell index 0–8
     */
    public static int getBestMove(Player[] board, Player aiPlayer, int difficulty) {
        List<Integer> legal = legalMoves(board);
        if (legal.isEmpty()) return -1;

        // Below 100%: sometimes pick a random move
        if (difficulty < 100 && RNG.nextInt(100) >= difficulty) {
            return legal.get(RNG.nextInt(legal.size()));
        }

        // Perfect minimax move
        Player human = (aiPlayer == Player.X) ? Player.O : Player.X;
        int bestScore = Integer.MIN_VALUE;
        int bestMove  = legal.get(0);

        for (int i : legal) {
            board[i] = aiPlayer;
            int score = minimax(board, false, aiPlayer, human);
            board[i] = null;
            if (score > bestScore) {
                bestScore = score;
                bestMove  = i;
            }
        }
        return bestMove;
    }

    // -------------------------------------------------------------------------

    private static int minimax(Player[] board, boolean isMaximizing,
                               Player aiPlayer, Player human) {
        Player winner = checkWinner(board);
        if (winner == aiPlayer) return 10;
        if (winner == human)    return -10;
        if (legalMoves(board).isEmpty()) return 0;

        int best = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i : legalMoves(board)) {
            board[i] = isMaximizing ? aiPlayer : human;
            int score = minimax(board, !isMaximizing, aiPlayer, human);
            board[i] = null;
            best = isMaximizing ? Math.max(best, score) : Math.min(best, score);
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

    private static List<Integer> legalMoves(Player[] board) {
        List<Integer> moves = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) moves.add(i);
        }
        return moves;
    }

    // -------------------------------------------------------------------------

    /** Maps a difficulty value (0–100) to a readable label. */
    public static String difficultyLabel(int value) {
        if (value <= 10)  return "Sehr leicht";
        if (value <= 35)  return "Leicht";
        if (value <= 60)  return "Mittel";
        if (value <= 85)  return "Schwer";
        if (value < 100)  return "Sehr schwer";
        return "Unschlagbar";
    }
}