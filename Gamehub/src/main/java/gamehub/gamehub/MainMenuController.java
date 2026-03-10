package gamehub.gamehub;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private BorderPane mainMenu;

    @FXML
    protected void onChessClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("chess/chess.fxml")
            );
            mainMenu.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onConnect4Click() {
        System.out.println("connect4");
    }

    @FXML
    protected void onTicTacToeClick() {
        System.out.println("TicTacToe");

    }

    @FXML
    protected void onSnakeClick() {
        System.out.println("Snake");
    }
}
