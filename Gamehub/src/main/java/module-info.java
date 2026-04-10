module gamehub.gamehub {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;


    opens gamehub.gamehub to javafx.fxml;
    opens gamehub.games.chess to javafx.fxml;
    exports gamehub.gamehub;
    opens gamehub.games.chess.figures to javafx.fxml;
    opens gamehub.games.ticTacToe to javafx.fxml;
    opens gamehub.games.connectFour to javafx.fxml;
    exports gamehub.games.chess;
}