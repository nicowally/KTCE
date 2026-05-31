package gamehub.games.snake_new;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Snakeserver {

    public static final int DEFAULT_PORT = 54321;
    public static final int GRID_W = 36;
    public static final int GRID_H = 26;

    private static final int MOVE_DELAY_MS  = 85;
    private static final int FRAME_DELAY_MS = 16;

    // ── Spielzustand ──────────────────────────────────────────────────────────
    private final List<int[]> snake1 = new ArrayList<>();
    private final List<int[]> snake2 = new ArrayList<>();

    private Direction dir1 = Direction.RIGHT;
    private Direction dir2 = Direction.LEFT;
    private Direction nextDir1 = Direction.RIGHT;
    private Direction nextDir2 = Direction.LEFT;

    private int[] food = {18, 13};
    private FruitType currentFruit = FruitType.APPLE;

    private int score1 = 0, score2 = 0;
    private boolean chaosMode = false;

    // Effekte Spieler 1
    private boolean shield1 = false, flip1 = false, speed1 = false, freeze1 = false;
    private long shieldEnd1, flipEnd1, speedEnd1, freezeEnd1;
    private int moveDelay1 = MOVE_DELAY_MS;

    // Effekte Spieler 2
    private boolean shield2 = false, flip2 = false, speed2 = false, freeze2 = false;
    private long shieldEnd2, flipEnd2, speedEnd2, freezeEnd2;
    private int moveDelay2 = MOVE_DELAY_MS;

    private long lastMove1, lastMove2;

    private final Random random = new Random();

    enum GamePhase { WAITING, RUNNING, GAME_OVER }
    private GamePhase phase = GamePhase.WAITING;
    private String gameOverMsg = "";

    // ── Netzwerk ──────────────────────────────────────────────────────────────
    private ClientHandler handler1, handler2;
    private int p1ColorIndex = 0;
    private int p2ColorIndex = 2; // Rot als Standard für P2

    private ServerSocket serverSocket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public enum Direction {
        UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
        final int dx, dy;
        Direction(int dx, int dy){ this.dx=dx; this.dy=dy; }
        boolean isOpposite(Direction o){ return dx==-o.dx && dy==-o.dy; }
    }

    public enum FruitType {
        APPLE, PEAR, ORANGE, GRAPES, CHERRY,
        GOLD_APPLE, ROTTEN_APPLE, ROTTEN_MEAT, LIGHTNING, ICE
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Snakeserver().start(port);
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[Server] Lauscht auf Port " + port);
        System.out.println("[Server] Warte auf 2 Spieler...");

        // Spieler 1
        Socket s1 = serverSocket.accept();
        System.out.println("[Server] Spieler 1 verbunden: " + s1.getInetAddress());
        handler1 = new ClientHandler(s1, 1);
        handler1.send("WAITING");

        // Spieler 2
        Socket s2 = serverSocket.accept();
        System.out.println("[Server] Spieler 2 verbunden: " + s2.getInetAddress());
        handler2 = new ClientHandler(s2, 2);

        // Farben austauschen
        handler1.send("COLORINFO:" + p1ColorIndex + ":" + p2ColorIndex);
        handler2.send("COLORINFO:" + p2ColorIndex + ":" + p1ColorIndex);

        // Lese-Threads starten
        new Thread(handler1::readLoop).start();
        new Thread(handler2::readLoop).start();

        // Spiel initialisieren und starten
        initGame();
        phase = GamePhase.RUNNING;
        handler1.send("START:" + p1ColorIndex + ":" + p2ColorIndex);
        handler2.send("START:" + p2ColorIndex + ":" + p1ColorIndex);

        // Game-Loop starten
        scheduler.scheduleAtFixedRate(this::gameTick, 0, FRAME_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void initGame() {
        snake1.clear();
        for (int i = 8; i >= 4; i--) snake1.add(new int[]{i, 8});

        snake2.clear();
        for (int i = 28; i <= 32; i++) snake2.add(new int[]{i, 18});

        dir1 = Direction.RIGHT; nextDir1 = Direction.RIGHT;
        dir2 = Direction.LEFT;  nextDir2 = Direction.LEFT;
        score1 = 0; score2 = 0;

        shield1=false; flip1=false; speed1=false; freeze1=false;
        shield2=false; flip2=false; speed2=false; freeze2=false;
        moveDelay1 = MOVE_DELAY_MS; moveDelay2 = MOVE_DELAY_MS;

        long now = System.currentTimeMillis();
        lastMove1 = now; lastMove2 = now;

        spawnFood();
    }

    // ── Game Loop ─────────────────────────────────────────────────────────────
    private synchronized void gameTick() {
        if (phase != GamePhase.RUNNING) return;
        long now = System.currentTimeMillis();

        tickEffects(now);

        boolean moved1 = false, moved2 = false;
        if (now - lastMove1 >= moveDelay1) { moveSnake(1); lastMove1 = now; moved1 = true; }
        if (now - lastMove2 >= moveDelay2) { moveSnake(2); lastMove2 = now; moved2 = true; }

        if (moved1 || moved2) broadcastState();
    }

    private void tickEffects(long now) {
        if (shield1 && now > shieldEnd1)  shield1 = false;
        if (flip1   && now > flipEnd1)    flip1   = false;
        if (freeze1 && now > freezeEnd1)  freeze1 = false;
        if (speed1  && now > speedEnd1) { speed1 = false; moveDelay1 = MOVE_DELAY_MS; }

        if (shield2 && now > shieldEnd2)  shield2 = false;
        if (flip2   && now > flipEnd2)    flip2   = false;
        if (freeze2 && now > freezeEnd2)  freeze2 = false;
        if (speed2  && now > speedEnd2) { speed2 = false; moveDelay2 = MOVE_DELAY_MS; }
    }

    private void moveSnake(int player) {
        List<int[]> snake  = player == 1 ? snake1 : snake2;
        Direction   nd     = player == 1 ? nextDir1 : nextDir2;
        Direction   cd     = player == 1 ? dir1     : dir2;
        boolean     frozen = player == 1 ? freeze1  : freeze2;
        boolean     flip   = player == 1 ? flip1    : flip2;
        boolean     shield = player == 1 ? shield1  : shield2;

        Direction applied = flip ? flipDir(nd) : nd;
        if (!applied.isOpposite(cd)) cd = applied;
        if (player == 1) dir1 = cd; else dir2 = cd;

        int[] head = snake.get(0);
        int nx = head[0] + cd.dx;
        int ny = head[1] + cd.dy;

        if (!frozen) {
            if (nx < 0 || nx >= GRID_W || ny < 0 || ny >= GRID_H) {
                triggerGameOver(player == 1 ? 2 : 1, "Wand"); return;
            }
        } else {
            nx = (nx + GRID_W) % GRID_W;
            ny = (ny + GRID_H) % GRID_H;
        }

        // Selbstkollision
        if (!shield) {
            List<int[]> otherSnake = player == 1 ? snake2 : snake1;
            for (int i = 0; i < snake.size(); i++) {
                if (!isFood(nx, ny) && i == snake.size()-1) continue;
                if (snake.get(i)[0] == nx && snake.get(i)[1] == ny) {
                    triggerGameOver(player == 1 ? 2 : 1, "Selbst"); return;
                }
            }
            // Kollision mit anderer Schlange
            for (int[] seg : otherSnake) {
                if (seg[0] == nx && seg[1] == ny) {
                    triggerGameOver(player == 1 ? 2 : 1, "Andere Schlange"); return;
                }
            }
        }

        // Kopf-zu-Kopf
        List<int[]> other = player == 1 ? snake2 : snake1;
        if (!other.isEmpty() && other.get(0)[0] == nx && other.get(0)[1] == ny) {
            triggerGameOver(0, "Kopf-zu-Kopf"); return;
        }

        boolean grow = isFood(nx, ny);

        // Schlange bewegen
        int[] newHead = {nx, ny};
        snake.add(0, newHead);
        if (!grow) snake.remove(snake.size() - 1);

        if (grow) {
            if (player == 1) { score1++; applyFruitEffect(1, currentFruit); }
            else              { score2++; applyFruitEffect(2, currentFruit); }

            if (currentFruit == FruitType.ROTTEN_MEAT) {
                // Shrink statt wachsen
                for (int i = 0; i < 3 && snake.size() > 3; i++) snake.remove(snake.size()-1);
                if (player == 1) score1 = Math.max(0, score1 - 3);
                else             score2 = Math.max(0, score2 - 3);
            }
            spawnFood();
        }
    }

    private boolean isFood(int x, int y) { return food[0] == x && food[1] == y; }

    private void applyFruitEffect(int player, FruitType type) {
        long now = System.currentTimeMillis();
        if (player == 1) {
            switch(type) {
                case GOLD_APPLE   -> { shield1=true; shieldEnd1=now+10_000; }
                case ROTTEN_APPLE -> { flip1=true;   flipEnd1  =now+ 8_000; }
                case LIGHTNING    -> { speed1=true; moveDelay1=MOVE_DELAY_MS/2; speedEnd1=now+6_000; }
                case ICE          -> { freeze2=true; freezeEnd2=now+5_000; } // friert GEGNER (P2) ein
                default -> {}
            }
        } else {
            switch(type) {
                case GOLD_APPLE   -> { shield2=true; shieldEnd2=now+10_000; }
                case ROTTEN_APPLE -> { flip2=true;   flipEnd2  =now+ 8_000; }
                case LIGHTNING    -> { speed2=true; moveDelay2=MOVE_DELAY_MS/2; speedEnd2=now+6_000; }
                case ICE          -> { freeze1=true; freezeEnd1=now+5_000; } // friert GEGNER (P1) ein
                default -> {}
            }
        }
    }

    private Direction flipDir(Direction d) {
        return switch(d) { case UP->Direction.DOWN; case DOWN->Direction.UP; case LEFT->Direction.RIGHT; case RIGHT->Direction.LEFT; };
    }

    private void spawnFood() {
        int x, y;
        do {
            x = random.nextInt(GRID_W);
            y = random.nextInt(GRID_H);
        } while (occupiedByAny(x, y));
        food = new int[]{x, y};
        pickFruit();
    }

    private boolean occupiedByAny(int x, int y) {
        for (int[] s : snake1) if (s[0]==x && s[1]==y) return true;
        for (int[] s : snake2) if (s[0]==x && s[1]==y) return true;
        return false;
    }

    private void pickFruit() {
        if (!chaosMode) {
            FruitType[] n = {FruitType.APPLE,FruitType.PEAR,FruitType.ORANGE,FruitType.GRAPES,FruitType.CHERRY};
            currentFruit = n[random.nextInt(n.length)];
        } else {
            int r = random.nextInt(100);
            if      (r<15) currentFruit = FruitType.GOLD_APPLE;
            else if (r<28) currentFruit = FruitType.ROTTEN_APPLE;
            else if (r<39) currentFruit = FruitType.ROTTEN_MEAT;
            else if (r<50) currentFruit = FruitType.LIGHTNING;
            else if (r<55) currentFruit = FruitType.ICE;
            else {
                FruitType[] n = {FruitType.APPLE,FruitType.PEAR,FruitType.ORANGE,FruitType.GRAPES,FruitType.CHERRY};
                currentFruit = n[random.nextInt(n.length)];
            }
        }
    }

    private synchronized void triggerGameOver(int winner, String reason) {
        if (phase == GamePhase.GAME_OVER) return;
        phase = GamePhase.GAME_OVER;
        gameOverMsg = winner == 0 ? "DRAW" : String.valueOf(winner);
        System.out.println("[Server] Game Over – Gewinner: " + gameOverMsg + " (" + reason + ")");
        broadcast("GAMEOVER:" + gameOverMsg);
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────
    private void broadcastState() {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("STATE:");
        sb.append("{");
        sb.append("\"s1\":").append(segListJson(snake1)).append(",");
        sb.append("\"s2\":").append(segListJson(snake2)).append(",");
        sb.append("\"f\":[").append(food[0]).append(",").append(food[1]).append("],");
        sb.append("\"ft\":\"").append(currentFruit.name()).append("\",");
        sb.append("\"sc1\":").append(score1).append(",");
        sb.append("\"sc2\":").append(score2).append(",");
        sb.append("\"eff1\":\"").append(effectString(1, now)).append("\",");
        sb.append("\"eff2\":\"").append(effectString(2, now)).append("\"");
        sb.append("}");
        broadcast(sb.toString());
    }

    private String effectString(int player, long now) {
        List<String> e = new ArrayList<>();
        if (player == 1) {
            if (shield1) e.add("SHIELD:" + (shieldEnd1 - now));
            if (flip1)   e.add("FLIP:"   + (flipEnd1   - now));
            if (speed1)  e.add("SPEED:"  + (speedEnd1  - now));
            if (freeze1) e.add("FREEZE:" + (freezeEnd1 - now));
        } else {
            if (shield2) e.add("SHIELD:" + (shieldEnd2 - now));
            if (flip2)   e.add("FLIP:"   + (flipEnd2   - now));
            if (speed2)  e.add("SPEED:"  + (speedEnd2  - now));
            if (freeze2) e.add("FREEZE:" + (freezeEnd2 - now));
        }
        return String.join(",", e);
    }

    private String segListJson(List<int[]> segs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < segs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(segs.get(i)[0]).append(",").append(segs.get(i)[1]).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private void broadcast(String msg) {
        if (handler1 != null) handler1.send(msg);
        if (handler2 != null) handler2.send(msg);
    }

    // ── Client Handler ────────────────────────────────────────────────────────
    class ClientHandler {
        final Socket socket;
        final int playerId;
        final PrintWriter out;
        final BufferedReader in;

        ClientHandler(Socket socket, int id) throws IOException {
            this.socket = socket;
            this.playerId = id;
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        void send(String msg) {
            out.println(msg);
        }

        void readLoop() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                System.out.println("[Server] Spieler " + playerId + " getrennt.");
                triggerGameOver(playerId == 1 ? 2 : 1, "Disconnect");
            }
        }

        private synchronized void handleMessage(String msg) {
            if (msg.startsWith("DIR:")) {
                String dirStr = msg.substring(4).trim();
                try {
                    Direction d = Direction.valueOf(dirStr);
                    if (playerId == 1) nextDir1 = d;
                    else               nextDir2 = d;
                } catch (IllegalArgumentException ignored) {}
            } else if (msg.equals("RESTART")) {
                if (phase == GamePhase.GAME_OVER) {
                    if (playerId == 1) {
                        host1ReadyForRestart = true;
                        // Host sendet RESTART → Settings-Phase für beide öffnen
                        // Beitreter bekommt RESTART_LOBBY damit er Farbe wählen kann
                        handler2.send("RESTART_LOBBY");
                        send("RESTART_LOBBY_HOST");
                    } else {
                        guest2ReadyForRestart = true;
                    }
                    // Wenn beide bereit → starten
                    if (host1ReadyForRestart && guest2ReadyForRestart) {
                        restartGame();
                    }
                }
            } else if (msg.equals("RESTART_CONFIRM")) {
                // Beitreter bestätigt dass er bereit ist (nach Farbwahl)
                if (phase == GamePhase.GAME_OVER && playerId == 2) {
                    guest2ReadyForRestart = true;
                    if (host1ReadyForRestart) restartGame();
                    else handler2.send("WAITING_FOR_HOST");
                }
            } else if (msg.startsWith("COLOR:")) {
                // Spieler wählt Farbe vor dem Start
                try {
                    int ci = Integer.parseInt(msg.substring(6).trim());
                    if (playerId == 1) p1ColorIndex = ci;
                    else               p2ColorIndex = ci;
                } catch (NumberFormatException ignored) {}
            } else if (msg.startsWith("CHAOS:")) {
                // Nur der Host (Spieler 1) darf den Chaos-Modus ändern
                if (playerId == 1) {
                    chaosMode = msg.substring(6).trim().equals("1");
                    // Beitreter informieren damit er es live sieht
                    broadcast("CHAOSINFO:" + (chaosMode ? "1" : "0"));
                }
            }
        }
    }

    private int restartVotes = 0;
    private boolean host1ReadyForRestart = false;
    private boolean guest2ReadyForRestart = false;

    private synchronized void restartGame() {
        initGame();
        phase = GamePhase.RUNNING;
        host1ReadyForRestart = false;
        guest2ReadyForRestart = false;
        // Farben aus den zuletzt gemeldeten Werten nehmen
        handler1.send("START:" + p1ColorIndex + ":" + p2ColorIndex);
        handler2.send("START:" + p2ColorIndex + ":" + p1ColorIndex);
    }
}