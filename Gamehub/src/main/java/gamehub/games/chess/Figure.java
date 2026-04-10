package gamehub.games.chess;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
import java.util.Map;

public abstract class Figure extends ImageView {

    public String type;
    public int col;
    public int row;
    private static final Map<String, Image> cache = new HashMap<>();

    public Figure(String type, int col, int row) {
        this.type = type;
        this.col = col;
        this.row = row;

        setImage(cache.computeIfAbsent(type, t -> new Image(getClass().getResourceAsStream("/gamehub/gamehub/games/chess/figure/" + t + ".png"))));
        setFitWidth(ChessBoard.SQUARE_SIZE);
        setFitHeight(ChessBoard.SQUARE_SIZE);
    }

    public static void preloadAll() {
        String[] types = {"wK","wQ","wB","wN","wR","wP","bK","bQ","bB","bN","bR","bP"};
        for (String type : types) {
            cache.computeIfAbsent(type, t -> new Image(Figure.class.getResourceAsStream("/gamehub/gamehub/games/chess/figure/" + t + ".png")));
        }
    }
    public boolean canMoveTo(int targetCol, int targetRow, ChessBoard board) {
        // unbekannte Figur darf sich nicht bewegen
        return false;
    }
}
