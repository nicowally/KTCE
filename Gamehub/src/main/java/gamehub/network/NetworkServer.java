package gamehub.network;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * The HOST side of a LAN session. <p>
 * Usage:<p>
 *   NetworkServer server = new NetworkServer();<p>
 *   server.setOnMessage(msg -> { /* handle on FX thread *​/ }); <p>
 *   server.setOnClientConnected(() -> { /* both players ready *​/ });<p>
 *   server.setOnDisconnected(() -> { /* client left *​/ });<p>
 *   server.start(5555);<p>
 *   // Later:
 *   server.send(new GameMessage(GameMessage.Type.MOVE, 3));<p>
 *   server.stop();
 */
public class NetworkServer {

    private static final int BACKLOG = 1;

    private ServerSocket          serverSocket;
    private Socket                clientSocket;
    private ObjectOutputStream    out;
    private ObjectInputStream     in;

    // Callbacks – always invoked on the JavaFX Application Thread
    private Consumer<GameMessage> onMessage;
    private Runnable              onClientConnected;
    private Runnable              onDisconnected;

    private volatile boolean running = false;

    // ── Public API ────────────────────────────────────────────────────────────

    public void setOnMessage(Consumer<GameMessage> handler)    { this.onMessage          = handler; }
    public void setOnClientConnected(Runnable handler)         { this.onClientConnected  = handler; }
    public void setOnDisconnected(Runnable handler)            { this.onDisconnected     = handler; }

    /**
     * Opens a ServerSocket on the given port and waits (on a background thread)
     * for exactly one client to connect.
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port, BACKLOG);
        running = true;

        Thread acceptThread = new Thread(() -> {
            try {
                // Blocks until the client connects
                clientSocket = serverSocket.accept();

                // Output stream MUST be created before input stream on both ends
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                in  = new ObjectInputStream(clientSocket.getInputStream());

                if (onClientConnected != null) {
                    Platform.runLater(onClientConnected);
                }

                listenLoop();

            } catch (IOException e) {
                if (running) {
                    // Unexpected disconnect
                    Platform.runLater(() -> { if (onDisconnected != null) onDisconnected.run(); });
                }
            }
        }, "LAN-Server-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Sends a message to the connected client.
     * Safe to call from the JavaFX thread.
     */
    public synchronized void send(GameMessage msg) {
        if (out == null) return;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // prevent object-graph caching between calls
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Returns the LAN IP address the server is bound to (best-effort). */
    public String getLocalAddress() {
        try {
            // Walk network interfaces to find a site-local (192.168.x / 10.x) address
            for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {}
        return "127.0.0.1";
    }

    /** Closes all sockets and stops the listen loop. */
    public void stop() {
        running = false;
        closeQuietly(clientSocket);
        closeQuietly(serverSocket);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Continuously reads messages from the client until the connection drops. */
    private void listenLoop() {
        try {
            while (running) {
                GameMessage msg = (GameMessage) in.readObject();
                if (onMessage != null) {
                    Platform.runLater(() -> onMessage.accept(msg));
                }
            }
        } catch (EOFException | SocketException e) {
            // Client disconnected cleanly or socket was closed
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

    private static void closeQuietly(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }
}