module gamehub.gamehub {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires javafx.graphics;


    opens gamehub.gamehub to javafx.fxml;
    opens gamehub.games.chess to javafx.fxml;
    exports gamehub.gamehub;
    opens gamehub.games.chess.figures to javafx.fxml;
    opens gamehub.games.ticTacToe to javafx.fxml;
    opens gamehub.games.connectFour to javafx.fxml;
    exports gamehub.games.chess;
    exports gamehub.games.chess.network;
    opens gamehub.games.chess.network to javafx.fxml;
}