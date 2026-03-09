module gamehub.gamehub {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens gamehub.gamehub to javafx.fxml;
    opens gamehub.games.chess to javafx.fxml;
    exports gamehub.gamehub;
}