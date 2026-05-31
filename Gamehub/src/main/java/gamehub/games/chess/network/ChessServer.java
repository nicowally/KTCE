package gamehub.games.chess.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {

    public static final int PORT = 55555;

    private ServerSocket serverSocket;
    private ClientHandler player1; // Weiß (Host)
    private ClientHandler player2;

    // Startet den Server — blockiert bis beide Spieler verbunden sind
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("[ChessServer] Lauscht auf Port " + PORT);

        System.out.println("[ChessServer] Warte auf Spieler 1 (Weiß)...");
        Socket s1 = serverSocket.accept();
        player1 = new ClientHandler(s1, "Spieler 1");
        System.out.println("[ChessServer] Spieler 1 verbunden: " + s1.getInetAddress());

        System.out.println("[ChessServer] Warte auf Spieler 2 (Schwarz)...");
        Socket s2 = serverSocket.accept();
        player2 = new ClientHandler(s2, "Spieler 2");
        System.out.println("[ChessServer] Spieler 2 verbunden: " + s2.getInetAddress());

        // Farben zuweisen
        player1.send(ChessNetworkMessage.buildAssignColor("w"));
        player2.send(ChessNetworkMessage.buildAssignColor("b"));

        // Spiel starten
        player1.send(ChessNetworkMessage.buildReady());
        player2.send(ChessNetworkMessage.buildReady());

        System.out.println("[ChessServer] Beide Spieler bereit — Spiel läuft!");

        // Beide Spieler in eigenen Threads laufen lassen
        Thread t1 = new Thread(() -> listen(player1, player2), "listener-p1");
        Thread t2 = new Thread(() -> listen(player2, player1), "listener-p2");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
    }

    // Hört auf Nachrichten von "sender" und leitet sie an "receiver" weiter
    private void listen(ClientHandler sender, ClientHandler receiver) {
        try {
            String line;
            while ((line = sender.readLine()) != null) {
                ChessNetworkMessage msg = ChessNetworkMessage.parse(line);
                if (msg == null) continue;

                switch (msg.type) {
                    case ChessNetworkMessage.MOVE,
                         ChessNetworkMessage.PROMOTION,
                         ChessNetworkMessage.CHECK,
                         ChessNetworkMessage.FORFEIT -> receiver.send(line);

                    case ChessNetworkMessage.GAME_OVER -> {
                        sender.send(line);
                        receiver.send(line);
                    }

                    default -> System.out.println("Fehler");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        receiver.send(ChessNetworkMessage.buildOpponentDisconnected());
        stop();
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static class ClientHandler {

        final String name;
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        ClientHandler(Socket socket, String name) throws IOException {
            this.socket = socket;
            this.name = name;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        void send(String message) {
            writer.println(message);
        }

        String readLine() throws IOException {
            return reader.readLine();
        }
    }
}