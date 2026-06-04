package gamehub.network;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * The CLIENT (joiner) side of a LAN session.
 * Usage:
 *   NetworkClient client = new NetworkClient();
 *   client.setOnMessage(msg -> { /* handle on FX thread *​/ });
 *   client.setOnConnected(() -> { /* handshake done *​/ });
 *   client.setOnDisconnected(() -> { /* host left *​/ });
 *   client.connect("192.168.1.42", 5555);
 *   // Later:
 *   client.send(new GameMessage(GameMessage.Type.MOVE, 3));
 *   client.stop();
 */
public class NetworkClient {

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Callbacks – always invoked on the JavaFX Application Thread
    private Consumer<GameMessage> onMessage;
    private Runnable              onConnected;
    private Runnable              onDisconnected;

    private volatile boolean running = false;

    // ── Public API ────────────────────────────────────────────────────────────

    public void setOnMessage(Consumer<GameMessage> handler)  { this.onMessage      = handler; }
    public void setOnConnected(Runnable handler)             { this.onConnected    = handler; }
    public void setOnDisconnected(Runnable handler)          { this.onDisconnected = handler; }

    /**
     * Connects to a host on a background thread, then starts listening.
     *
     * @param host hostname or IP address of the server
     * @param port port the server is listening on
     */
    public void connect(String host, int port) {
        Thread t = new Thread(() -> {
            try {
                socket  = new Socket(host, port);
                running = true;

                // Output stream MUST be flushed before the input stream is opened
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in  = new ObjectInputStream(socket.getInputStream());

                if (onConnected != null) {
                    Platform.runLater(onConnected);
                }

                listenLoop();

            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> { if (onDisconnected != null) onDisconnected.run(); });
                } else {
                    // Connection attempt itself failed – treat as disconnected
                    Platform.runLater(() -> { if (onDisconnected != null) onDisconnected.run(); });
                }
                e.printStackTrace();
            }
        }, "LAN-Client-Connect");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Sends a message to the server.
     * Safe to call from the JavaFX thread.
     */
    public synchronized void send(GameMessage msg) {
        if (out == null) return;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // prevent stale cached object graphs
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Closes the socket and stops the listen loop. */
    public void stop() {
        running = false;
        if (socket != null) try { socket.close(); } catch (IOException ignored) {}
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void listenLoop() {
        try {
            while (running) {
                GameMessage msg = (GameMessage) in.readObject();
                if (onMessage != null) {
                    Platform.runLater(() -> onMessage.accept(msg));
                }
            }
        } catch (EOFException | SocketException e) {
            if (running) {
                Platform.runLater(() -> { if (onDisconnected != null) onDisconnected.run(); });
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                e.printStackTrace();
                Platform.runLater(() -> { if (onDisconnected != null) onDisconnected.run(); });
            }
        }
    }
}