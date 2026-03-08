package SnakeNew.Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SnakeServer {
    private static final int PORT = 5000;
    private static final int GRID_WIDTH = 36;
    private static final int GRID_HEIGHT = 26;
    private static final int MOVE_DELAY_MS = 90;

    private final ServerSocket serverSocket;
    private final ClientHandler[] clients = new ClientHandler[2];
    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    private final Object lock = new Object();

    private final List<Point> snake1 = new ArrayList<>();
    private final List<Point> snake2 = new ArrayList<>();

    private Direction dir1 = Direction.RIGHT;
    private Direction dir2 = Direction.LEFT;
    private Direction nextDir1 = Direction.RIGHT;
    private Direction nextDir2 = Direction.LEFT;

    private Point food = new Point(18, 13);
    private int score1 = 0;
    private int score2 = 0;
    private String status = "WAITING";

    public SnakeServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        resetRound();
    }

    public void start() throws IOException {
        System.out.println("Snake server läuft auf Port " + PORT);
        System.out.println("Warte auf 2 Spieler...");

        for (int i = 0; i < 2; i++) {
            Socket socket = serverSocket.accept();
            clients[i] = new ClientHandler(socket, i + 1);
            new Thread(clients[i], "client-" + (i + 1)).start();
            System.out.println("Spieler " + (i + 1) + " verbunden: " + socket.getInetAddress());
        }

        synchronized (lock) {
            status = "RUNNING";
        }
        broadcastState();

        loop.scheduleAtFixedRate(this::tickSafe, 0, MOVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void tickSafe() {
        try {
            tick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tick() {
        synchronized (lock) {
            if (!"RUNNING".equals(status)) {
                broadcastState();
                return;
            }

            if (!nextDir1.isOpposite(dir1)) dir1 = nextDir1;
            if (!nextDir2.isOpposite(dir2)) dir2 = nextDir2;

            Point newHead1 = snake1.isEmpty() ? null : new Point(snake1.get(0).x + dir1.dx, snake1.get(0).y + dir1.dy);
            Point newHead2 = snake2.isEmpty() ? null : new Point(snake2.get(0).x + dir2.dx, snake2.get(0).y + dir2.dy);

            boolean grow1 = newHead1 != null && newHead1.equals(food);
            boolean grow2 = newHead2 != null && newHead2.equals(food);

            boolean dead1 = isWall(newHead1) || collides(newHead1, snake1, !grow1) || collides(newHead1, snake2, false);
            boolean dead2 = isWall(newHead2) || collides(newHead2, snake2, !grow2) || collides(newHead2, snake1, false);

            if (newHead1 != null && newHead1.equals(newHead2)) {
                dead1 = true;
                dead2 = true;
            }

            if (!dead1) moveSnake(snake1, newHead1, grow1);
            if (!dead2) moveSnake(snake2, newHead2, grow2);

            if (grow1 && !dead1 && !grow2) {
                score1++;
                spawnFood();
            } else if (grow2 && !dead2 && !grow1) {
                score2++;
                spawnFood();
            } else if (grow1 && grow2 && !dead1 && !dead2) {
                spawnFood();
            }

            if (dead1 || dead2) {
                if (dead1 && dead2) {
                    status = "DRAW";
                } else if (dead1) {
                    status = "P2_WIN";
                } else {
                    status = "P1_WIN";
                }
            }

            broadcastState();
        }
    }

    private boolean isWall(Point p) {
        return p == null || p.x < 0 || p.y < 0 || p.x >= GRID_WIDTH || p.y >= GRID_HEIGHT;
    }

    private boolean collides(Point head, List<Point> snake, boolean excludeTail) {
        if (head == null) return true;

        int limit = snake.size();
        if (excludeTail && limit > 0) limit--;

        for (int i = 0; i < limit; i++) {
            if (head.equals(snake.get(i))) return true;
        }
        return false;
    }

    private void moveSnake(List<Point> snake, Point newHead, boolean grow) {
        snake.add(0, newHead);
        if (!grow) {
            snake.remove(snake.size() - 1);
        }
    }

    private void resetRound() {
        synchronized (lock) {
            snake1.clear();
            snake2.clear();

            snake1.add(new Point(8, 13));
            snake1.add(new Point(7, 13));
            snake1.add(new Point(6, 13));

            snake2.add(new Point(27, 13));
            snake2.add(new Point(28, 13));
            snake2.add(new Point(29, 13));

            dir1 = Direction.RIGHT;
            dir2 = Direction.LEFT;
            nextDir1 = Direction.RIGHT;
            nextDir2 = Direction.LEFT;

            score1 = 0;
            score2 = 0;
            status = clients[0] != null && clients[1] != null ? "RUNNING" : "WAITING";

            spawnFood();
        }
    }

    private void spawnFood() {
        Point candidate;
        do {
            candidate = new Point(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT));
        } while (snake1.contains(candidate) || snake2.contains(candidate));
        food = candidate;
    }

    private void handleInput(int playerId, String input) {
        synchronized (lock) {
            switch (input) {
                case "UP" -> {
                    if (playerId == 1 && dir1 != Direction.DOWN) nextDir1 = Direction.UP;
                    if (playerId == 2 && dir2 != Direction.DOWN) nextDir2 = Direction.UP;
                }
                case "DOWN" -> {
                    if (playerId == 1 && dir1 != Direction.UP) nextDir1 = Direction.DOWN;
                    if (playerId == 2 && dir2 != Direction.UP) nextDir2 = Direction.DOWN;
                }
                case "LEFT" -> {
                    if (playerId == 1 && dir1 != Direction.RIGHT) nextDir1 = Direction.LEFT;
                    if (playerId == 2 && dir2 != Direction.RIGHT) nextDir2 = Direction.LEFT;
                }
                case "RIGHT" -> {
                    if (playerId == 1 && dir1 != Direction.LEFT) nextDir1 = Direction.RIGHT;
                    if (playerId == 2 && dir2 != Direction.LEFT) nextDir2 = Direction.RIGHT;
                }
                case "RESTART" -> {
                    if (!"RUNNING".equals(status)) {
                        resetRound();
                        status = clients[0] != null && clients[1] != null ? "RUNNING" : "WAITING";
                    }
                }
            }
        }
    }

    private void broadcastState() {
        String state = serializeState();
        for (ClientHandler client : clients) {
            if (client != null) client.send(state);
        }
    }

    private String serializeState() {
        return "STATE|" + status + "|" + score1 + "|" + score2 + "|" + food.x + "|" + food.y + "|"
                + encodeSnake(snake1) + "|" + encodeSnake(snake2);
    }

    private String encodeSnake(List<Point> snake) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            if (i > 0) sb.append(';');
            sb.append(p.x).append(',').append(p.y);
        }
        return sb.toString();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private final BufferedReader in;
        private final PrintWriter out;

        ClientHandler(Socket socket, int playerId) throws IOException {
            this.socket = socket;
            this.playerId = playerId;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        @Override
        public void run() {
            send("WELCOME|" + playerId);
            send("INFO|Waiting for state...");
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleInput(playerId, line.trim().toUpperCase(Locale.ROOT));
                }
            } catch (IOException e) {
                System.out.println("Spieler " + playerId + " getrennt.");
            }
        }

        void send(String msg) {
            out.println(msg);
        }
    }

    private record Point(int x, int y) {}

    private enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        boolean isOpposite(Direction other) {
            return dx == -other.dx && dy == -other.dy;
        }
    }

    public static void main(String[] args) throws Exception {
        new SnakeServer().start();
    }
}