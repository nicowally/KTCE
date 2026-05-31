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
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Spielmodus wählen");
        choiceAlert.setHeaderText("Wie möchtest du Schach spielen?");
        choiceAlert.setContentText("Wähle einen Modus:");
        ButtonType chessLocal = new ButtonType("Lokal (2 Spieler)");
        ButtonType chessLan = new ButtonType("LAN (Netzwerk)");
        ButtonType chessCancel = new ButtonType("Abbrechen");

        choiceAlert.getButtonTypes().setAll(chessLocal, chessLan, chessCancel);

        Optional<ButtonType> result = choiceAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == chessLocal) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            GamehubApplication.class.getResource("/gamehub/gamehub/games/chess/chess.fxml")
                    );
                    mainMenu.getScene().setRoot(loader.load());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (result.get() == chessLan) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            GamehubApplication.class.getResource("/gamehub/gamehub/games/chess/chessLAN.fxml")
                    );
                    mainMenu.getScene().setRoot(loader.load());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
        // Ask the user whether they want to play against another player or the AI
        ButtonType twoPlayerBtn = new ButtonType("Gegen Spieler");
        ButtonType aiBtn        = new ButtonType("Gegen KI");
        ButtonType cancelBtn    = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Tic-Tac-Toe");
        alert.setHeaderText("Gegen wen möchtest du spielen?");
        alert.setContentText("Wähle einen Spielmodus.");
        alert.getButtonTypes().setAll(twoPlayerBtn, aiBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == cancelBtn) return;

        boolean vsAI = result.get() == aiBtn;

        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("/gamehub/gamehub/games/ticTacToe/game.fxml")
            );
            // Load the FXML – this also calls TicTacToeController.initialize()
            javafx.scene.Parent root = loader.load();

            // Pass the mode to the controller, then call postInit()
            TicTacToeController controller = loader.getController();
            controller.initMode(vsAI);
            controller.postInit();

            mainMenu.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSnakeClick() {
        ButtonType classicButton = new ButtonType("Classic");
        ButtonType modernButton  = new ButtonType("Modern");
        ButtonType cancelButton  = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

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