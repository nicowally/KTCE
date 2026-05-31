package gamehub.games.chess.network;

public class ChessNetworkMessage {

    public static final String ASSIGN_COLOR = "ASSIGN_COLOR";
    public static final String READY = "READY";
    public static final String MOVE = "MOVE";
    public static final String PROMOTION = "PROMOTION";
    public static final String CHECK = "CHECK";
    public static final String FORFEIT = "FORFEIT";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String OPPONENT_DISCONNECTED = "OPPONENT_DISCONNECTED";

    public final String type;
    private final String[] parts; // z.B. ["MOVE", "e2", "e4"]

    private ChessNetworkMessage(String[] parts) {
        this.type = parts[0];
        this.parts = parts;
    }

    // Koordinaten werden als Schachnotation übergeben, z.B. col=4, row=6 → "e2"
    public static String buildMove(int fromCol, int fromRow, int toCol, int toRow) {
        return MOVE + ":" + toNotation(fromCol, fromRow) + ":" + toNotation(toCol, toRow);
    }

    public static String buildPromotion(String choice) {
        return PROMOTION + ":" + choice;
    }

    public static String buildCheck() {
        return CHECK;
    }

    public static String buildForfeit(String color) {
        return FORFEIT + ":" + color;
    }

    public static String buildAssignColor(String color) {
        return ASSIGN_COLOR + ":" + color;
    }

    public static String buildReady() {
        return READY;
    }

    public static String buildOpponentDisconnected() {
        return OPPONENT_DISCONNECTED;
    }

    public static ChessNetworkMessage parse(String line) {
        if (line == null || line.isBlank()) return null;
        return new ChessNetworkMessage(line.trim().split(":"));
    }

    // Gibt einen Teil der Nachricht zurück, z.B. getPart(1) bei "MOVE:e2:e4" → "e2"
    public String getPart(int index) {
        return parts[index];
    }

    // Wandelt einen Schach-Koordinaten-String zurück in Spalte, z.B. "e2" → col=4
    public int getCol(int partIndex) {
        return parts[partIndex].charAt(0) - 'a'; // 'a'=0, 'b'=1, ... 'h'=7
    }

    // Wandelt einen Schach-Koordinaten-String zurück in Zeile, z.B. "e2" → row=6
    public int getRow(int partIndex) {
        return 8 - (parts[partIndex].charAt(1) - '0'); // "2"→6, "8"→0
    }

    // col=4, row=6 → "e2"
    private static String toNotation(int col, int row) {
        return "" + (char)('a' + col) + (8 - row);
    }
}

