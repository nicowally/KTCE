package gamehub.gamehub;

import gamehub.games.snake_new.SnakeGameNew;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * SnakeModernWrapper – zeigt zuerst ein Lobby-Menü:
 *   [Einzelspieler]   [Multiplayer hosten]   [Multiplayer beitreten]
 *
 * Multiplayer hosten: Startet einen SnakeServer im Hintergrund auf Port 54321,
 *   dann verbindet sich der lokale Client damit (localhost).
 *
 * Multiplayer beitreten: IP-Eingabe → verbindet sich mit dem Host.
 */
public class SnakeModernWrapper {

    // Letzte Server-Instanz merken damit sie beim erneuten Hosten sauber gestoppt wird
    private static gamehub.games.snake_new.Snakeserver runningServer = null;

    public static BorderPane createSnakePane() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a26;");

        Button backButton = new Button("← Zurück");
        backButton.setStyle(
                "-fx-background-color: #2a2a3a; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 6 14; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        backButton.setOnAction(e -> {
            // Server stoppen wenn man zurück geht
            if (runningServer != null) {
                runningServer.stop();
                runningServer = null;
            }
            try {
                FXMLLoader loader = new FXMLLoader(
                        GamehubApplication.class.getResource("main-menu.fxml")
                );
                root.getScene().setRoot(loader.load());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(8, 12, 8, 12));
        topBar.setStyle("-fx-background-color: #111118;");
        root.setTop(topBar);

        // Zeige zuerst das Lobby-Menü
        showLobby(root);

        return root;
    }

    // ── Lobby ─────────────────────────────────────────────────────────────────
    private static void showLobby(BorderPane root) {
        VBox lobby = new VBox(20);
        lobby.setAlignment(Pos.CENTER);
        lobby.setStyle("-fx-background-color: #1a1a26;");

        Label title = new Label("🐍 SNAKE");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 42));
        title.setTextFill(Color.web("#82cc68"));

        Label sub = new Label("Wähle deinen Spielmodus");
        sub.setFont(Font.font("SansSerif", 16));
        sub.setTextFill(Color.web("#aaaaaa"));

        // Einzelspieler
        Button btnSingle = createLobbyButton("🎮  Einzelspieler", "#224422", "#44cc44");
        btnSingle.setOnAction(e -> startSinglePlayer(root));

        // Host
        Button btnHost = createLobbyButton("🌐  Multiplayer hosten", "#222244", "#4488ff");
        btnHost.setOnAction(e -> showHostDialog(root));

        // Join
        Button btnJoin = createLobbyButton("🔗  Multiplayer beitreten", "#332222", "#ff8844");
        btnJoin.setOnAction(e -> showJoinDialog(root));

        Label hint = new Label("Beim Hosten: Gib deine IP-Adresse an den Mitspieler weiter.");
        hint.setFont(Font.font("SansSerif", 12));
        hint.setTextFill(Color.web("#666677"));

        lobby.getChildren().addAll(title, sub, btnSingle, btnHost, btnJoin, hint);
        root.setCenter(lobby);
    }

    private static Button createLobbyButton(String text, String bg, String border) {
        Button btn = new Button(text);
        btn.setPrefWidth(300);
        btn.setPrefHeight(52);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
                "-fx-background-color: " + bg + "; " +
                        "-fx-border-color: " + border + "; " +
                        "-fx-border-width: 2; -fx-border-radius: 12; " +
                        "-fx-background-radius: 12; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + border + "55; " +
                        "-fx-border-color: " + border + "; " +
                        "-fx-border-width: 2; -fx-border-radius: 12; " +
                        "-fx-background-radius: 12; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bg + "; " +
                        "-fx-border-color: " + border + "; " +
                        "-fx-border-width: 2; -fx-border-radius: 12; " +
                        "-fx-background-radius: 12; -fx-cursor: hand;"
        ));
        return btn;
    }

    // ── Einzelspieler ─────────────────────────────────────────────────────────
    private static void startSinglePlayer(BorderPane root) {
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> {
            SnakeGameNew snakeGame = new SnakeGameNew();
            swingNode.setContent(snakeGame);
            SwingUtilities.invokeLater(snakeGame::requestFocusInWindow);
        });
        Platform.runLater(() -> root.setCenter(swingNode));
    }

    // ── Host Dialog ───────────────────────────────────────────────────────────
    private static void showHostDialog(BorderPane root) {
        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1a1a26;");

        Label title = new Label("Server hosten");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#4488ff"));

        // IP-Anzeige
        String localIp = getLocalIp();
        Label ipLabel = new Label("Deine IP:  " + localIp);
        ipLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 17));
        ipLabel.setTextFill(Color.web("#44ff88"));
        ipLabel.setStyle("-fx-background-color: #1a2a1a; -fx-padding: 8 16; -fx-background-radius: 8;");

        Label ipHint = new Label("Gib diese IP an deinen Mitspieler weiter.");
        ipHint.setFont(Font.font("SansSerif", 13));
        ipHint.setTextFill(Color.web("#888899"));

        Label portLabel = new Label("Port:  54321");
        portLabel.setFont(Font.font("SansSerif", 13));
        portLabel.setTextFill(Color.web("#888899"));

        Label chaosHint = new Label("💡 Du (Host) bestimmst den Chaos-Modus für beide Spieler.");
        chaosHint.setFont(Font.font("SansSerif", 12));
        chaosHint.setTextFill(Color.web("#aa88ff"));

        Label statusLabel = new Label("Drücke 'Server starten', um auf Mitspieler zu warten.");
        statusLabel.setFont(Font.font("SansSerif", 13));
        statusLabel.setTextFill(Color.web("#aaaacc"));

        Button btnStart = createLobbyButton("▶  Server starten & warten", "#112233", "#4488ff");
        Button btnBack  = createLobbyButton("← Zurück", "#1a1a26", "#555566");

        btnBack.setOnAction(e -> showLobby(root));

        btnStart.setOnAction(e -> {
            btnStart.setDisable(true);
            statusLabel.setText("Starte Server auf Port 54321 ...");
            new Thread(() -> {
                try {
                    // Windows-Firewall-Regel automatisch hinzufügen (einmalig, braucht Admin-Rechte)
                    tryAddFirewallRule();

                    // Alten Server sauber beenden falls noch einer läuft
                    if (runningServer != null) {
                        runningServer.stop();
                        runningServer = null;
                        Thread.sleep(300);
                    }

                    // Auf Windows: Prozess der Port 54321 belegt automatisch beenden
                    forceReleasePort(gamehub.games.snake_new.Snakeserver.DEFAULT_PORT);

                    // Server in Hintergrund-Thread starten
                    // Wir nutzen ein Flag das der Server setzt sobald der ServerSocket offen ist
                    gamehub.games.snake_new.Snakeserver server =
                            new gamehub.games.snake_new.Snakeserver();
                    runningServer = server;
                    Thread serverThread = new Thread(() -> {
                        try { server.start(gamehub.games.snake_new.Snakeserver.DEFAULT_PORT); }
                        catch (Exception ex) {
                            Platform.runLater(() -> statusLabel.setText("Server-Fehler: " + ex.getMessage()));
                        }
                    }, "SnakeServer");
                    serverThread.setDaemon(true);  // Automatisch beenden wenn Programm endet
                    serverThread.start();

                    // Warten bis der Server sein isReady-Flag gesetzt hat (max 5 Sekunden)
                    // WICHTIG: Kein Test-Socket! Der würde als Spieler 1 gezählt werden.
                    int waited = 0;
                    while (!server.isReady() && waited < 5000) {
                        Thread.sleep(50);
                        waited += 50;
                    }
                    if (!server.isReady()) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Server konnte nicht gestartet werden!");
                            btnStart.setDisable(false);
                        });
                        return;
                    }

                    Platform.runLater(() -> statusLabel.setText("Server läuft! Verbinde als Spieler 1..."));

                    // Als Client verbinden
                    Platform.runLater(() -> startMultiplayerClient(root, "localhost", 1));

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Fehler: " + ex.getMessage());
                        btnStart.setDisable(false);
                    });
                }
            }, "HostSetup").start();
        });

        box.getChildren().addAll(title, ipLabel, ipHint, portLabel, chaosHint, statusLabel, btnStart, btnBack);
        root.setCenter(box);
    }

    // ── Join Dialog ───────────────────────────────────────────────────────────
    private static void showJoinDialog(BorderPane root) {
        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1a1a26;");

        Label title = new Label("Spiel beitreten");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#ff8844"));

        Label ipHint = new Label("IP-Adresse des Hosts eingeben:");
        ipHint.setFont(Font.font("SansSerif", 14));
        ipHint.setTextFill(Color.web("#aaaacc"));

        TextField ipField = new TextField("192.168.1.xxx");
        ipField.setPrefWidth(260);
        ipField.setStyle(
                "-fx-background-color: #222233; -fx-text-fill: white; " +
                        "-fx-font-size: 15px; -fx-padding: 8 12; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-border-color: #ff8844; -fx-border-width: 1;"
        );
        ipField.setOnMouseClicked(e -> {
            if (ipField.getText().startsWith("192.168")) ipField.clear();
        });

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", 13));
        statusLabel.setTextFill(Color.web("#ff8844"));

        Button btnJoin = createLobbyButton("🔗  Verbinden", "#332211", "#ff8844");
        Button btnBack = createLobbyButton("← Zurück", "#1a1a26", "#555566");

        btnBack.setOnAction(e -> showLobby(root));

        btnJoin.setOnAction(e -> {
            String host = ipField.getText().trim();
            if (host.isEmpty()) { statusLabel.setText("Bitte IP eingeben!"); return; }
            btnJoin.setDisable(true);
            statusLabel.setText("Verbinde mit " + host + ":54321 ...");
            Platform.runLater(() -> startMultiplayerClient(root, host, 2));
        });

        box.getChildren().addAll(title, ipHint, ipField, statusLabel, btnJoin, btnBack);
        root.setCenter(box);
    }

    // ── Multiplayer Client starten ────────────────────────────────────────────
    private static void startMultiplayerClient(BorderPane root, String host, int playerId) {
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> {
            SnakeGameNew snakeGame = new SnakeGameNew();
            snakeGame.setMyPlayerId(playerId);
            snakeGame.connectToServer(host, gamehub.games.snake_new.Snakeserver.DEFAULT_PORT);
            swingNode.setContent(snakeGame);
            SwingUtilities.invokeLater(snakeGame::requestFocusInWindow);
        });
        Platform.runLater(() -> root.setCenter(swingNode));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    /**
     * Versucht eine Windows-Firewall-Regel für Port 54321 hinzuzufügen.
     * Schlägt still fehl wenn keine Admin-Rechte vorhanden sind oder kein Windows-System.
     * Auf Linux/Mac ist keine Aktion nötig (Ports sind standardmäßig offen).
     */
    /**
     * Findet auf Windows den Prozess der den angegebenen Port belegt und beendet ihn.
     * Verhindert "Address already in use" beim erneuten Server-Start.
     */
    private static void forceReleasePort(int port) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) return;

            // netstat -ano gibt alle Verbindungen mit PIDs aus
            Process netstat = Runtime.getRuntime().exec(
                    new String[]{"cmd", "/c", "netstat -ano | findstr :" + port}
            );
            netstat.waitFor();
            String output = new String(netstat.getInputStream().readAllBytes());

            // PID aus der letzten Spalte extrahieren
            java.util.Set<String> pids = new java.util.HashSet<>();
            for (String line : output.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Nur LISTENING oder ESTABLISHED Zeilen mit unserem Port
                if (!line.contains(":" + port)) continue;
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String pid = parts[parts.length - 1];
                    if (pid.matches("\\d+") && !pid.equals("0")) {
                        pids.add(pid);
                    }
                }
            }

            for (String pid : pids) {
                System.out.println("[Server] Beende Prozess PID " + pid + " der Port " + port + " belegt.");
                Runtime.getRuntime().exec(new String[]{"taskkill", "/PID", pid, "/F"}).waitFor();
            }

            if (!pids.isEmpty()) Thread.sleep(500); // kurz warten nach dem Kill

        } catch (Exception e) {
            System.out.println("[Server] Port-Freigabe fehlgeschlagen: " + e.getMessage());
        }
    }

    private static void tryAddFirewallRule() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return; // Nur auf Windows nötig
        try {
            // Prüfen ob Regel schon existiert
            Process check = Runtime.getRuntime().exec(new String[]{
                    "netsh", "advfirewall", "firewall", "show", "rule", "name=SnakeMultiplayer"
            });
            check.waitFor();
            if (check.exitValue() == 0) return; // Regel existiert bereits

            // Neue Regel hinzufügen
            Runtime.getRuntime().exec(new String[]{
                    "netsh", "advfirewall", "firewall", "add", "rule",
                    "name=SnakeMultiplayer",
                    "dir=in",
                    "action=allow",
                    "protocol=TCP",
                    "localport=54321"
            });
            System.out.println("[Server] Firewall-Regel für Port 54321 hinzugefügt.");
        } catch (Exception ex) {
            System.out.println("[Server] Firewall-Regel konnte nicht gesetzt werden: " + ex.getMessage());
            System.out.println("[Server] Bitte Port 54321 TCP manuell in der Windows-Firewall freigeben.");
        }
    }

    private static String getLocalIp() {
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (Exception e) {
            // Fallback: alle Netzwerk-Interfaces durchsuchen
            try {
                java.util.Enumeration<java.net.NetworkInterface> ifaces =
                        java.net.NetworkInterface.getNetworkInterfaces();
                while (ifaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = ifaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        java.net.InetAddress addr = addrs.nextElement();
                        if (addr instanceof java.net.Inet4Address) return addr.getHostAddress();
                    }
                }
            } catch (Exception ignored) {}
            return "127.0.0.1";
        }
    }
}