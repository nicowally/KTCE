package gamehub.gamehub;

import gamehub.chess.Figure;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GamehubApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GamehubApplication.class.getResource("main-menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 600);
        stage.setTitle("GameHub");
        stage.setMaximized(true);
        stage.setResizable(false);
        stage.setScene(scene);

        Figure.preloadAll();

        stage.show();
    }
}
