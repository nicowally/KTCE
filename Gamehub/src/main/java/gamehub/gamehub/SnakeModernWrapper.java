package gamehub.gamehub;

import gamehub.games.snake_new.SnakeGameNew;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import javax.swing.SwingUtilities;
import java.io.IOException;

public class SnakeModernWrapper {

    public static BorderPane createSnakePane() {
        BorderPane root = new BorderPane();

        Button backButton = new Button("Zurück");
        backButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        GamehubApplication.class.getResource("main-menu.fxml")
                );
                root.getScene().setRoot(loader.load());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        SwingNode swingNode = new SwingNode();

        SwingUtilities.invokeLater(() -> {
            SnakeGameNew snakeGame = new SnakeGameNew();
            swingNode.setContent(snakeGame);
            SwingUtilities.invokeLater(snakeGame::requestFocusInWindow);
        });

        root.setTop(backButton);
        root.setCenter(swingNode);

        return root;
    }
}