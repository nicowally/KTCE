package gamehub.games.chess.network;

import java.io.*;
import java.net.Socket;

public class ChessClient {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private String myColor;

    // Über dieses Interface informiert der Client den Controller über eingehende Nachrichten
    public interface MessageListener {
        void onMessage(ChessNetworkMessage message);
    }

    private MessageListener listener;

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public String getMyColor() {
        return myColor;
    }

    // Verbindet sich zum Server — blockiert kurz bis die Verbindung steht
    public void connect(String host) throws IOException {
        socket = new Socket(host, ChessServer.PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        System.out.println("[ChessClient] Verbunden mit " + host + ":" + ChessServer.PORT);

        // Listener-Thread starten
        Thread listenerThread = new Thread(this::listenLoop, "chess-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Sendet eine fertig gebaute Nachricht an den Server
    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Läuft im Hintergrund und leitet jede eingehende Nachricht an den Listener weiter
    private void listenLoop() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                ChessNetworkMessage msg = ChessNetworkMessage.parse(line);
                if (msg == null) continue;

                System.out.println("[ChessClient] Empfangen: " + line);

                if (msg.type.equals(ChessNetworkMessage.ASSIGN_COLOR)) {
                    myColor = msg.getPart(1);
                    System.out.println("[ChessClient] Ich spiele: " + myColor);
                    continue;
                }
                if (listener != null) {
                    listener.onMessage(msg);
                }
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onMessage(ChessNetworkMessage.parse(
                        ChessNetworkMessage.buildOpponentDisconnected()
                ));
            }
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}
