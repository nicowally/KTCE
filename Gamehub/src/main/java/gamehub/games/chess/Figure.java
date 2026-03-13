package gamehub.games.chess;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
import java.util.Map;

public class Figure extends ImageView {

    public String type;
    public int col;
    public int row;
    private static final Map<String, Image> cache = new HashMap<>();

    public Figure(String type, int col, int row) {
        this.type = type;
        this.col = col;
        this.row = row;

        setImage(cache.computeIfAbsent(type, t -> new Image(getClass().getResourceAsStream("/gamehub/gamehub/chess/figure/" + t + ".png"))));
        setFitWidth(Board.SQUARE_SIZE);
        setFitHeight(Board.SQUARE_SIZE);
    }

    public static void preloadAll() {
        String[] types = {"wK","wQ","wB","wN","wR","wP","bK","bQ","bB","bN","bR","bP"};
        for (String type : types) {
            cache.computeIfAbsent(type, t -> new Image(Figure.class.getResourceAsStream("/gamehub/gamehub/chess/figure/" + t + ".png")));
        }
    }
    public boolean canMoveTo(int targetCol, int targetRow, Board board) {
        // unbekannte Figur darf sich nicht bewegen
        return false;
    }
}
