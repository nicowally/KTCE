module gamehub.gamehub {
    requires javafx.controls;
    requires javafx.fxml;


    opens gamehub.gamehub to javafx.fxml;
    exports gamehub.gamehub;
}