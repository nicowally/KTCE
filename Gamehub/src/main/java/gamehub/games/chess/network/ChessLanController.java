package gamehub.games.chess.network;

import gamehub.gamehub.GamehubApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.InetAddress;

public class ChessLanController {

    @FXML private BorderPane root;
    @FXML private Label hostInfoLabel;
    @FXML private Button hostButton;
    @FXML private TextField ipField;
    @FXML private Button joinButton;
    @FXML private Label joinStatusLabel;

    private ChessServer runningServer = null;

    @FXML
    private void onHostClick() {
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            hostInfoLabel.setText("Warten auf Gegner...\nDeine IP: " + ip);
        } catch (Exception e) {
            hostInfoLabel.setText("Warten auf Gegner...\n(IP konnte nicht ermittelt werden)");
        }

        runningServer = new ChessServer();

        // Server auf eigenem Thread starten (blockiert bis beide Spieler verbunden)
        Thread serverThread = new Thread(() -> {
            try {
                runningServer.start();
            } catch (IOException e) {
                Platform.runLater(() -> {
                    hostInfoLabel.setText("Server-Fehler: " + e.getMessage());
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }
        }, "chess-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        Thread hostClientThread = new Thread(() -> {
            try {
                // Warten bis der Server-Socket offen ist
                Thread.sleep(300);

                ChessClient client = new ChessClient();
                client.connect("localhost");

                waitForColor(client);

                client.setMessageListener(msg -> {
                    if (msg.type.equals(ChessNetworkMessage.READY)) {
                        Platform.runLater(() -> launchChessBoard(client));
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    hostInfoLabel.setText("Fehler: " + e.getMessage());
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            } catch (InterruptedException ignored) {}
        }, "chess-host-client-thread");
        hostClientThread.setDaemon(true);
        hostClientThread.start();
    }

    @FXML
    private void onJoinClick() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            joinStatusLabel.setText("Bitte eine IP-Adresse eingeben.");
            return;
        }

        joinButton.setDisable(true);
        hostButton.setDisable(true);
        joinStatusLabel.setText("Verbinde mit " + ip + "...");

        Thread joinThread = new Thread(() -> {
            try {
                ChessClient client = new ChessClient();
                client.setMessageListener(msg -> {
                    if (msg.type.equals(ChessNetworkMessage.READY)) {
                        Platform.runLater(() -> launchChessBoard(client));
                    }
                });
                client.connect(ip);

                waitForColor(client);

            } catch (IOException e) {
                Platform.runLater(() -> {
                    joinStatusLabel.setText("Verbindung fehlgeschlagen:\n" + e.getMessage());
                    joinButton.setDisable(false);
                    hostButton.setDisable(false);
                });
            }
        }, "chess-join-thread");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    private void launchChessBoard(ChessClient client) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("/gamehub/gamehub/games/chess/chess.fxml")
            );
            root.getScene().setRoot(loader.load());

            ChessNetworkController controller = loader.getController();
            controller.initNetwork(client);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClick() {
        if (runningServer != null) {
            runningServer.stop();
            runningServer = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("main-menu.fxml")
            );
            root.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Wartet kurz bis ASSIGN_COLOR vom Server verarbeitet wurde
    private void waitForColor(ChessClient client) {
        int attempts = 0;
        while (client.getMyColor() == null && attempts < 20) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            attempts++;
        }
    }
}