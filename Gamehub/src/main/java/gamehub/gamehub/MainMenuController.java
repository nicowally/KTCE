package gamehub.gamehub;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.io.IOException;

public class MainMenuController {

    @FXML
    private BorderPane mainMenu;

    @FXML
    protected void onChessClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("/gamehub/gamehub/games/chess/chess.fxml")
            );
            mainMenu.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onConnect4Click() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("/gamehub/gamehub/games/connectFour/connect4.fxml")
            );
            mainMenu.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onTicTacToeClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("/gamehub/gamehub/games/ticTacToe/game.fxml")
            );
            mainMenu.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSnakeClick() {
        ButtonType classicButton = new ButtonType("Classic");
        ButtonType modernButton = new ButtonType("Modern");
        ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Snake auswählen");
        alert.setHeaderText("Welche Snake-Version möchtest du spielen?");
        alert.setContentText("Wähle zwischen Classic und Modern.");

        alert.getButtonTypes().setAll(classicButton, modernButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == classicButton) {
                BorderPane snakePane = SnakeClassicWrapper.createSnakePane();
                mainMenu.getScene().setRoot(snakePane);
            } else if (result.get() == modernButton) {
                BorderPane snakePane = SnakeModernWrapper.createSnakePane();
                mainMenu.getScene().setRoot(snakePane);
            }
        }
    }
}
