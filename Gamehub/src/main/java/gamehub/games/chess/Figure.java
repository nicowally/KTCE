package gamehub.games.chess;

import gamehub.gamehub.ChessBoard;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.HashMap;
import java.util.Map;

public abstract class Figure extends ImageView {

    public String type;
    public int col;
    public int row;
    public static final Map<String, Image> cache = new HashMap<>();
    public boolean hasMoved = false;

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
    public abstract boolean canMoveTo(int targetCol, int targetRow, BoardLogic logic);
}
