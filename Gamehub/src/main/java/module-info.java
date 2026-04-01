module gamehub.gamehub {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;


    opens gamehub.gamehub to javafx.fxml;
    opens gamehub.chess to javafx.fxml;
    exports gamehub.gamehub;
    opens gamehub.chess.figures to javafx.fxml;
}