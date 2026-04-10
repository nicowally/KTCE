package gamehub.gamehub;

import gamehub.games.chess.Figure;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
public class GamehubApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GamehubApplication.class.getResource("main-menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("GameHub");
        stage.setScene(scene);

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setWidth(screen.getWidth());
        stage.setHeight(screen.getHeight());
        stage.setResizable(false);
        stage.setMaximized(true);

        Figure.preloadAll();

        stage.show();
    }
}
