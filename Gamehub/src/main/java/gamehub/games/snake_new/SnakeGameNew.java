package gamehub.games.snake_new;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class SnakeGameNew extends JPanel implements ActionListener, KeyListener {

    // ===== Grid =====
    public static final int GRID_WIDTH  = 36;
    public static final int GRID_HEIGHT = 26;

    // ===== Timing =====
    private static final int FRAME_DELAY_MS     = 16;
    private static final int MOVE_DELAY_MS_BASE = 85;

    private final Timer frameTimer;
    private long lastMoveTime;

    // ===== Single-Player Spielstatus =====
    private final List<SnakeSegment> snake = new ArrayList<>();
    private final Random random = new Random();

    private Point     foodGrid;
    private FruitType currentFruit;

    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection    = Direction.RIGHT;

    private enum GameState { LOBBY, RUNNING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.LOBBY;

    private int score = 0;
    private boolean chaosMode = false;

    private boolean shieldActive    = false;
    private boolean controlsFlipped = false;
    private boolean speedBoost      = false;
    private boolean freezeActive    = false;

    private long shieldEndTime   = 0;
    private long controlsEndTime = 0;
    private long speedEndTime    = 0;
    private long freezeEndTime   = 0;

    private int moveDelayMs = MOVE_DELAY_MS_BASE;

    // ===== Settings-Panel =====
    private final SettingsPanel settingsPanel;

    // ===== Farb-Optionen =====
    public static final SnakeColor[] SNAKE_COLORS = {
            new SnakeColor("Grün",   new Color( 34,118, 52), new Color( 82,204,104), new Color( 88,210,108)),
            new SnakeColor("Blau",   new Color( 25, 80,160), new Color( 70,160,240), new Color( 80,170,250)),
            new SnakeColor("Rot",    new Color(160, 30, 30), new Color(240, 80, 80), new Color(250, 90, 90)),
            new SnakeColor("Lila",   new Color(100, 30,150), new Color(190,100,240), new Color(200,110,250)),
            new SnakeColor("Orange", new Color(180, 90,  0), new Color(255,170, 50), new Color(255,180, 60)),
            new SnakeColor("Cyan",   new Color( 10,120,140), new Color( 60,220,230), new Color( 70,230,240)),
            new SnakeColor("Pink",   new Color(160, 30,100), new Color(255,110,180), new Color(255,120,190)),
            new SnakeColor("Weiß",   new Color(130,130,130), new Color(230,230,230), new Color(240,240,240)),
    };
    private int selectedColorIndex = 0;

    // ===== Multiplayer =====
    private boolean multiplayerMode = false;
    private int     myPlayerId      = 0;  // 1 oder 2

    // Empfangener Spielzustand vom Server
    private volatile List<int[]> mpSnake1 = new ArrayList<>();
    private volatile List<int[]> mpSnake2 = new ArrayList<>();
    private volatile int[]  mpFood   = {18, 13};
    private volatile String mpFruitType = "APPLE";
    private volatile int    mpScore1 = 0, mpScore2 = 0;
    private volatile String mpEff1 = "", mpEff2 = "";
    private volatile int    mpMyColor = 0, mpOtherColor = 2;

    // Interpolations-Puffer (vorherige und aktuelle Positionen)
    private List<int[]> mpSnake1Prev = new ArrayList<>();
    private List<int[]> mpSnake2Prev = new ArrayList<>();
    private long mpLastStateTime = 0;

    private enum MpState { CONNECTING, WAITING, WAITING_RESTART, PLAYING, GAME_OVER }
    private volatile MpState mpState = MpState.CONNECTING;
    private volatile String  mpWinner = "";
    private volatile String  mpStatusMsg = "Verbinde...";

    private Socket          mpSocket;
    private PrintWriter     mpOut;
    private BufferedReader  mpIn;

    // ===== Konstruktor =====
    public SnakeGameNew() {
        setPreferredSize(new Dimension(1100, 800));
        setFocusable(true);
        addKeyListener(this);

        settingsPanel = new SettingsPanel(this);
        add(settingsPanel);

        gameState = GameState.LOBBY;
        settingsPanel.setVisible(true);

        frameTimer = new Timer(FRAME_DELAY_MS, this);
        frameTimer.start();
    }

    // ===== Multiplayer Connect =====

    public void connectToServer(String host, int port) {
        multiplayerMode = true;
        settingsPanel.setVisible(false);
        mpState = MpState.CONNECTING;
        mpStatusMsg = "Verbinde mit " + host + ":" + port + " ...";
        repaint();

        new Thread(() -> {
            try {
                mpSocket = new Socket(host, port);
                mpOut = new PrintWriter(new OutputStreamWriter(mpSocket.getOutputStream()), true);
                mpIn  = new BufferedReader(new InputStreamReader(mpSocket.getInputStream()));
                mpStatusMsg = "Verbunden! Warte auf zweiten Spieler...";
                mpState = MpState.WAITING;
                repaint();
                networkReadLoop();
            } catch (IOException e) {
                mpStatusMsg = "Verbindungsfehler: " + e.getMessage();
                mpState = MpState.CONNECTING;
                SwingUtilities.invokeLater(this::repaint);
            }
        }, "MP-Read").start();
    }

    private void networkReadLoop() {
        try {
            String line;
            while ((line = mpIn.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            if (mpState != MpState.GAME_OVER) {
                mpStatusMsg = "Verbindung unterbrochen!";
                mpState = MpState.GAME_OVER;
                mpWinner = "?";
                SwingUtilities.invokeLater(this::repaint);
            }
        }
    }

    private void handleServerMessage(String msg) {
        if (msg.equals("WAITING")) {
            mpState = MpState.WAITING;
            mpStatusMsg = "Warte auf zweiten Spieler...";
        } else if (msg.startsWith("COLORINFO:")) {
            String[] p = msg.substring(10).split(":");
            mpMyColor    = Integer.parseInt(p[0]);
            mpOtherColor = Integer.parseInt(p[1]);
            selectedColorIndex = mpMyColor;
        } else if (msg.startsWith("START:")) {
            String[] p = msg.substring(6).split(":");
            mpMyColor    = Integer.parseInt(p[0]);
            mpOtherColor = Integer.parseInt(p[1]);
            selectedColorIndex = mpMyColor;
            mpState = MpState.PLAYING;
            mpWinner = "";
        } else if (msg.startsWith("STATE:")) {
            parseState(msg.substring(6));
        } else if (msg.startsWith("GAMEOVER:")) {
            mpWinner = msg.substring(9);
            mpState = MpState.GAME_OVER;
        } else if (msg.equals("WAITING_RESTART")) {
            mpState = MpState.WAITING_RESTART;
            mpStatusMsg = "Warte auf anderen Spieler für Neustart...";
        }
        repaint();
    }

    private void parseState(String json) {
        // Einfacher manueller Parser für das kompakte JSON-Format
        try {
            mpSnake1Prev = new ArrayList<>(mpSnake1);
            mpSnake2Prev = new ArrayList<>(mpSnake2);
            mpLastStateTime = System.currentTimeMillis();

            mpSnake1 = parseSegList(extractField(json, "s1"));
            mpSnake2 = parseSegList(extractField(json, "s2"));

            String fArr = extractField(json, "f");
            String[] fp = fArr.replaceAll("[\\[\\]]","").split(",");
            mpFood = new int[]{Integer.parseInt(fp[0].trim()), Integer.parseInt(fp[1].trim())};

            mpFruitType = extractStringField(json, "ft");
            mpScore1    = Integer.parseInt(extractField(json, "sc1").trim());
            mpScore2    = Integer.parseInt(extractField(json, "sc2").trim());
            mpEff1      = extractStringField(json, "eff1");
            mpEff2      = extractStringField(json, "eff2");
        } catch (Exception ignored) {}
    }

    private String extractField(String json, String key) {
        // Findet "key": VALUE bis zum nächsten Komma/} (für Zahlen und Arrays)
        String search = "\"" + key + "\":";
        int si = json.indexOf(search);
        if (si < 0) return "";
        int vi = si + search.length();
        char first = json.charAt(vi);
        if (first == '[') {
            // Finde passendes ]
            int depth = 0, ei = vi;
            while (ei < json.length()) {
                char c = json.charAt(ei);
                if (c == '[') depth++;
                if (c == ']') { depth--; if (depth == 0) { ei++; break; } }
                ei++;
            }
            return json.substring(vi, ei);
        }
        int end = json.indexOf(',', vi);
        if (end < 0) end = json.indexOf('}', vi);
        return end < 0 ? json.substring(vi) : json.substring(vi, end);
    }

    private String extractStringField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int si = json.indexOf(search);
        if (si < 0) return "";
        int vi = si + search.length();
        int end = json.indexOf('"', vi);
        return end < 0 ? "" : json.substring(vi, end);
    }

    private List<int[]> parseSegList(String arr) {
        List<int[]> list = new ArrayList<>();
        if (arr.isEmpty() || arr.equals("[]")) return list;
        String inner = arr.substring(1, arr.length()-1).trim();
        if (inner.isEmpty()) return list;
        // Finde alle [x,y]
        int i = 0;
        while (i < inner.length()) {
            int start = inner.indexOf('[', i);
            if (start < 0) break;
            int end = inner.indexOf(']', start);
            if (end < 0) break;
            String pair = inner.substring(start+1, end);
            String[] parts = pair.split(",");
            if (parts.length == 2) {
                list.add(new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())});
            }
            i = end + 1;
        }
        return list;
    }

    // ===== Helper Types =====

    public static class SnakeColor {
        final String name;
        final Color  outer, inner, head;
        SnakeColor(String name, Color outer, Color inner, Color head) {
            this.name=name; this.outer=outer; this.inner=inner; this.head=head;
        }
    }

    private enum Direction {
        UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
        final int dx, dy;
        Direction(int dx, int dy) { this.dx=dx; this.dy=dy; }
        boolean isOpposite(Direction o) { return dx==-o.dx && dy==-o.dy; }
        double angle() { return Math.atan2(dy,dx); }
    }

    public enum FruitType {
        APPLE, PEAR, ORANGE, GRAPES, CHERRY,
        GOLD_APPLE, ROTTEN_APPLE, ROTTEN_MEAT, LIGHTNING, ICE
    }

    static class SnakeSegment {
        int gridX, gridY, prevGridX, prevGridY;
        SnakeSegment(int x, int y) { gridX=x; gridY=y; prevGridX=x; prevGridY=y; }
        void setPosition(int x, int y) { prevGridX=gridX; prevGridY=gridY; gridX=x; gridY=y; }
    }

    private static class RenderPoint {
        final float x, y;
        RenderPoint(float x, float y) { this.x=x; this.y=y; }
    }

    // ===== Layout-Helpers =====
    private int getTileSize()    { return Math.max(1, Math.min(getWidth()/GRID_WIDTH, getHeight()/GRID_HEIGHT)); }
    private int getBoardWidth()  { return getTileSize() * GRID_WIDTH; }
    private int getBoardHeight() { return getTileSize() * GRID_HEIGHT; }
    private int getOffsetX()     { return (getWidth()  - getBoardWidth())  / 2; }
    private int getOffsetY()     { return (getHeight() - getBoardHeight()) / 2; }

    // ===== Spielstart (Single-Player) =====
    public void startGame() {
        if (multiplayerMode) return;
        snake.clear();
        for (int i=8; i>=4; i--) snake.add(new SnakeSegment(i, 8));

        currentDirection = Direction.RIGHT;
        nextDirection    = Direction.RIGHT;
        score = 0;

        shieldActive=false; controlsFlipped=false; speedBoost=false; freezeActive=false;
        moveDelayMs = MOVE_DELAY_MS_BASE;
        chaosMode = settingsPanel.isChaosMode();

        spawnFood();
        lastMoveTime = System.currentTimeMillis();

        gameState = GameState.RUNNING;
        settingsPanel.setVisible(false);
    }

    private void spawnFood() {
        Point c;
        do { c = new Point(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT)); }
        while (isOccupiedBySnake(c.x, c.y));
        foodGrid = c;
        pickFruitType();
    }

    private void pickFruitType() {
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

    private boolean isOccupiedBySnake(int x, int y) {
        for (SnakeSegment s : snake) if (s.gridX==x && s.gridY==y) return true;
        return false;
    }

    // ===== Game Loop =====
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!multiplayerMode) {
            if (gameState == GameState.RUNNING) {
                tickEffects();
                long now = System.currentTimeMillis();
                if (now - lastMoveTime >= moveDelayMs) { updateGame(); lastMoveTime=now; }
            }
        }
        repaint();
    }

    private void tickEffects() {
        long now = System.currentTimeMillis();
        if (shieldActive    && now>shieldEndTime)   shieldActive    = false;
        if (controlsFlipped && now>controlsEndTime) controlsFlipped = false;
        if (freezeActive    && now>freezeEndTime)   freezeActive    = false;
        if (speedBoost      && now>speedEndTime)  { speedBoost=false; moveDelayMs=MOVE_DELAY_MS_BASE; }
    }

    private void updateGame() {
        Direction applied = nextDirection;
        if (controlsFlipped) applied = flipDirection(applied);
        if (!applied.isOpposite(currentDirection)) currentDirection = applied;

        SnakeSegment head = snake.get(0);
        int newX = head.gridX + currentDirection.dx;
        int newY = head.gridY + currentDirection.dy;

        if (!freezeActive) {
            if (newX<0||newX>=GRID_WIDTH||newY<0||newY>=GRID_HEIGHT) {
                triggerGameOver(); return;
            }
        } else {
            newX = (newX+GRID_WIDTH)  % GRID_WIDTH;
            newY = (newY+GRID_HEIGHT) % GRID_HEIGHT;
        }

        boolean willGrow = (newX==foodGrid.x && newY==foodGrid.y);

        if (!shieldActive) {
            for (int i=0; i<snake.size(); i++) {
                if (!willGrow && i==snake.size()-1) continue;
                SnakeSegment s=snake.get(i);
                if (s.gridX==newX && s.gridY==newY) { triggerGameOver(); return; }
            }
        }

        int[] oldX=new int[snake.size()], oldY=new int[snake.size()];
        for (int i=0; i<snake.size(); i++) { oldX[i]=snake.get(i).gridX; oldY[i]=snake.get(i).gridY; }
        snake.get(0).setPosition(newX, newY);
        for (int i=1; i<snake.size(); i++) snake.get(i).setPosition(oldX[i-1], oldY[i-1]);

        if (willGrow) {
            applyFruitEffect(currentFruit);
            if (currentFruit != FruitType.ROTTEN_MEAT) {
                SnakeSegment t=new SnakeSegment(oldX[oldX.length-1], oldY[oldY.length-1]);
                t.prevGridX=oldX[oldX.length-1]; t.prevGridY=oldY[oldY.length-1];
                snake.add(t);
            }
            score++;
            spawnFood();
        }
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        settingsPanel.setVisible(true);
    }

    private void applyFruitEffect(FruitType type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case GOLD_APPLE   -> { shieldActive=true;    shieldEndTime   =now+10_000; }
            case ROTTEN_APPLE -> { controlsFlipped=true; controlsEndTime =now+ 8_000; }
            case LIGHTNING    -> { speedBoost=true; moveDelayMs=MOVE_DELAY_MS_BASE/2; speedEndTime=now+6_000; }
            case ICE          -> { freezeActive=true;    freezeEndTime   =now+ 5_000; }
            case ROTTEN_MEAT  -> {
                for (int i=0; i<3 && snake.size()>3; i++) snake.remove(snake.size()-1);
                score = Math.max(0, score - 3);
            }
            default -> {}
        }
    }

    private Direction flipDirection(Direction d) {
        return switch(d) {
            case UP -> Direction.DOWN; case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT; case RIGHT -> Direction.LEFT;
        };
    }

    // ===== Render =====
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        double time = System.currentTimeMillis()/1000.0;

        if (multiplayerMode) {
            drawBackground(g2, time);
            paintMultiplayer(g2, time);
        } else {
            drawBackground(g2, time);
            if (gameState == GameState.LOBBY) {
                drawLobbyOverlay(g2);
            } else {
                float progress = getMoveProgress();
                drawFood(g2, time);
                drawSnakeWrapped(g2, progress, time);
                drawHud(g2);
                drawEffectOverlays(g2);
                if (gameState == GameState.PAUSED)   drawCenteredOverlay(g2, "PAUSED",    "SPACE = Weiter");
                if (gameState == GameState.GAME_OVER) drawCenteredOverlay(g2, "GAME OVER", "R = Neu starten");
            }
        }

        g2.dispose();
        if (!multiplayerMode) layoutSettingsPanel();
    }

    // ── Multiplayer Paint ────────────────────────────────────────────────────
    private void paintMultiplayer(Graphics2D g2, double time) {
        switch (mpState) {
            case CONNECTING, WAITING -> drawMpWaitScreen(g2);
            case PLAYING             -> drawMpGame(g2, time);
            case WAITING_RESTART     -> drawMpWaitScreen(g2);
            case GAME_OVER           -> { drawMpGame(g2, time); drawMpGameOver(g2); }
        }
    }

    private void drawMpWaitScreen(Graphics2D g2) {
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), bh=getBoardHeight();
        g2.setColor(new Color(0,0,0,120)); g2.fillRect(bx,by,bw,bh);
        int boxW=Math.min(400,bw-40), boxH=130;
        int boxX=bx+(bw-boxW)/2, boxY=by+(bh-boxH)/2;
        g2.setColor(new Color(20,28,22,230)); g2.fillRoundRect(boxX,boxY,boxW,boxH,28,28);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(boxX,boxY,boxW,boxH,28,28);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,22));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString("MULTIPLAYER", bx+(bw-fm.stringWidth("MULTIPLAYER"))/2, boxY+42);
        g2.setFont(new Font("SansSerif",Font.PLAIN,13)); fm=g2.getFontMetrics();
        g2.setColor(new Color(180,240,180));
        g2.drawString(mpStatusMsg, bx+(bw-fm.stringWidth(mpStatusMsg))/2, boxY+78);
        // Pulsierender Rand
        double pulse = 0.5+0.5*Math.sin(System.currentTimeMillis()/400.0);
        g2.setColor(new Color(100,200,100,(int)(60+80*pulse)));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(boxX,boxY,boxW,boxH,28,28);
    }

    private void drawMpGame(Graphics2D g2, double time) {
        // Futter
        drawFoodMp(g2, time);

        // Beide Schlangen aus Serverdaten
        boolean iAm1 = myPlayerId == 1;
        List<int[]> mySegs    = iAm1 ? mpSnake1 : mpSnake2;
        List<int[]> otherSegs = iAm1 ? mpSnake2 : mpSnake1;
        int myColor    = mpMyColor;
        int otherColor = mpOtherColor;

        // Andere Schlange zuerst (damit meine oben liegt)
        drawNetSnake(g2, otherSegs, SNAKE_COLORS[otherColor], time, false);
        drawNetSnake(g2, mySegs,    SNAKE_COLORS[myColor],    time, true);

        // HUD
        drawMpHud(g2);
    }

    private void drawNetSnake(Graphics2D g2, List<int[]> segs, SnakeColor col, double time, boolean isMe) {
        if (segs.isEmpty()) return;
        int ts=getTileSize(), bx=getOffsetX(), by=getOffsetY();

        List<RenderPoint> pts = new ArrayList<>();
        for (int[] s : segs) {
            pts.add(new RenderPoint(bx + s[0]*ts + ts/2f, by + s[1]*ts + ts/2f));
        }
        pts = applySlither(pts, time);

        // Clip
        Shape oldClip = g2.getClip();
        g2.setClip(bx, by, getBoardWidth(), getBoardHeight());

        int t = ts;
        Path2D.Float path = buildSmoothPath(pts);

        // Schatten
        AffineTransform old = g2.getTransform();
        g2.translate(Math.max(2,t*0.15), Math.max(3,t*0.2));
        g2.setColor(new Color(0,0,0,50));
        g2.setStroke(new BasicStroke(Math.max(8f,t*0.95f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path);
        g2.setTransform(old);

        // Körper
        g2.setColor(col.outer);
        g2.setStroke(new BasicStroke(Math.max(7f,t*0.85f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path);
        g2.setColor(col.inner);
        g2.setStroke(new BasicStroke(Math.max(4f,t*0.5f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path);

        // Mein Spieler – leichter Glow
        if (isMe) {
            g2.setColor(new Color(col.inner.getRed(), col.inner.getGreen(), col.inner.getBlue(), 40));
            g2.setStroke(new BasicStroke(Math.max(12f,t*1.2f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }

        // Kopf
        drawNetHead(g2, pts, col, time, t);

        g2.setClip(oldClip);
    }

    private void drawNetHead(Graphics2D g2, List<RenderPoint> pts, SnakeColor col, double time, int t) {
        if (pts.isEmpty()) return;
        RenderPoint head = pts.get(0);
        double angle = 0;
        if (pts.size() >= 2) {
            RenderPoint next = pts.get(1);
            angle = Math.atan2(head.y - next.y, head.x - next.x);
        }
        float headW=Math.max(14f,t*1.2f), headH=Math.max(10f,t*0.95f);
        AffineTransform old=g2.getTransform();
        g2.translate(head.x, head.y);
        g2.rotate(angle);
        g2.setColor(new Color(0,0,0,40));
        g2.fill(new Ellipse2D.Float(-headW/2f+2,-headH/2f+3,headW,headH));
        g2.setColor(col.outer);
        g2.fill(new Ellipse2D.Float(-headW/2f,-headH/2f,headW,headH));
        g2.setColor(col.head);
        g2.fill(new Ellipse2D.Float(-headW/2f+2,-headH/2f+2,headW-4,headH-4));
        g2.setColor(new Color(255,255,255,45));
        g2.fill(new Ellipse2D.Float(-3,-6,8,4));
        // Augen
        float es=Math.max(3.2f,t*0.24f), ps=Math.max(1.4f,t*0.1f);
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(t*0.12f-es/2f,-t*0.26f-es/2f,es,es));
        g2.fill(new Ellipse2D.Float(t*0.12f-es/2f, t*0.26f-es/2f,es,es));
        g2.setColor(Color.BLACK);
        g2.fill(new Ellipse2D.Float(t*0.12f-ps/2f,-t*0.26f-ps/2f,ps,ps));
        g2.fill(new Ellipse2D.Float(t*0.12f-ps/2f, t*0.26f-ps/2f,ps,ps));
        g2.setTransform(old);
    }

    private void drawFoodMp(Graphics2D g2, double time) {
        if (mpFood == null) return;
        int t=getTileSize(), bx=getOffsetX(), by=getOffsetY();
        int x=bx+mpFood[0]*t, y=by+mpFood[1]*t;
        float bob=(float)Math.sin(time*3.0+mpFood[0]*0.7+mpFood[1]*0.4)*Math.max(1f,t*0.08f);
        Graphics2D gf=(Graphics2D)g2.create(); gf.translate(0,bob);
        gf.setColor(new Color(0,0,0,35));
        gf.fillOval(x+t/4,y+(int)(t*0.72),t*2/3,Math.max(3,t/5));
        FruitType ft;
        try { ft = FruitType.valueOf(mpFruitType); } catch(Exception e) { ft = FruitType.APPLE; }
        switch (ft) {
            case APPLE -> drawApple(gf,x,y,t);
            case PEAR  -> drawPear(gf,x,y,t);
            case ORANGE -> drawOrange(gf,x,y,t);
            case GRAPES -> drawGrapes(gf,x,y,t);
            case CHERRY -> drawCherry(gf,x,y,t);
            case GOLD_APPLE   -> drawGoldApple(gf,x,y,t,time);
            case ROTTEN_APPLE -> drawRottenApple(gf,x,y,t,time);
            case ROTTEN_MEAT  -> drawRottenMeat(gf,x,y,t,time);
            case LIGHTNING    -> drawLightning(gf,x,y,t,time);
            case ICE          -> drawIceFruit(gf,x,y,t,time);
        }
        gf.dispose();
    }

    private void drawMpHud(Graphics2D g2) {
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), t=getTileSize();

        // Spieler 1 (links)
        SnakeColor c1 = SNAKE_COLORS[Math.min(mpMyColor, SNAKE_COLORS.length-1)];
        int hudW=Math.max(160,t*9), hudH=Math.max(38,t*2);
        g2.setColor(new Color(c1.outer.getRed(), c1.outer.getGreen(), c1.outer.getBlue(), 160));
        g2.fillRoundRect(bx+8,by+8,hudW,hudH,14,14);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(14,t)));
        boolean iAm1 = myPlayerId == 1;
        String p1Label = iAm1 ? "Du: " : "Gegner: ";
        g2.drawString(p1Label + mpScore1, bx+22, by+8+hudH/2+6);

        // Spieler 2 (rechts)
        SnakeColor c2 = SNAKE_COLORS[Math.min(mpOtherColor, SNAKE_COLORS.length-1)];
        String p2Label = iAm1 ? "Gegner: " : "Du: ";
        FontMetrics fm = g2.getFontMetrics();
        String p2Str = p2Label + mpScore2;
        int p2w = fm.stringWidth(p2Str)+32;
        g2.setColor(new Color(c2.outer.getRed(), c2.outer.getGreen(), c2.outer.getBlue(), 160));
        g2.fillRoundRect(bx+bw-p2w-8, by+8, p2w, hudH, 14, 14);
        g2.setColor(Color.WHITE);
        g2.drawString(p2Str, bx+bw-p2w, by+8+hudH/2+6);

        // Effekte eigener Spieler
        drawMpEffects(g2);

        // Steuerhinweis
        g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(11,t*2/3)));
        g2.setColor(new Color(255,255,255,130));
        g2.drawString("WASD / Pfeiltasten steuern",bx+12,by+getBoardHeight()-8);
    }

    private void drawMpEffects(Graphics2D g2) {
        String effStr = (myPlayerId == 1) ? mpEff1 : mpEff2;
        if (effStr == null || effStr.isEmpty()) return;
        int x=getOffsetX(), y=getOffsetY()+getBoardHeight()+6;
        int barW=120, barH=16, gap=8;
        g2.setFont(new Font("SansSerif",Font.BOLD,11));
        for (String eff : effStr.split(",")) {
            if (eff.isEmpty()) continue;
            String[] parts = eff.split(":");
            if (parts.length < 2) continue;
            long rem;
            try { rem = Long.parseLong(parts[1]); } catch(NumberFormatException e) { continue; }
            switch (parts[0]) {
                case "SHIELD" -> { drawBar(g2,x,y,barW,barH,new Color(255,215,0),"Schutz",  rem/10_000f); x+=barW+gap; }
                case "FLIP"   -> { drawBar(g2,x,y,barW,barH,new Color(160,50,200),"Invertiert",rem/8_000f); x+=barW+gap; }
                case "SPEED"  -> { drawBar(g2,x,y,barW,barH,new Color(255,220,0),"Speed",   rem/6_000f); x+=barW+gap; }
                case "FREEZE" -> { drawBar(g2,x,y,barW,barH,new Color(100,200,255),"Freeze", rem/5_000f); x+=barW+gap; }
            }
        }
    }

    private void drawMpGameOver(Graphics2D g2) {
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), bh=getBoardHeight();
        g2.setColor(new Color(0,0,0,130)); g2.fillRect(bx,by,bw,bh);
        int boxW=Math.min(360,bw-40), boxH=160;
        int boxX=bx+(bw-boxW)/2, boxY=by+(bh-boxH)/2;
        g2.setColor(new Color(28,36,28,230)); g2.fillRoundRect(boxX,boxY,boxW,boxH,28,28);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(boxX,boxY,boxW,boxH,28,28);

        String title;
        if (mpWinner.equals("DRAW")) title = "UNENTSCHIEDEN";
        else {
            boolean iWon = (myPlayerId == 1 && mpWinner.equals("1")) || (myPlayerId == 2 && mpWinner.equals("2"));
            title = iWon ? "DU GEWINNST!" : "DU VERLIERST!";
        }
        g2.setColor(mpWinner.equals("DRAW") ? Color.YELLOW : (title.startsWith("DU G") ? new Color(100,255,100) : new Color(255,100,100)));
        g2.setFont(new Font("SansSerif",Font.BOLD,28));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(title, bx+(bw-fm.stringWidth(title))/2, boxY+52);

        g2.setColor(new Color(200,200,200)); g2.setFont(new Font("SansSerif",Font.PLAIN,14)); fm=g2.getFontMetrics();
        String sub = "Score: " + mpScore1 + " – " + mpScore2;
        g2.drawString(sub, bx+(bw-fm.stringWidth(sub))/2, boxY+82);

        g2.setColor(new Color(130,230,130)); g2.setFont(new Font("SansSerif",Font.BOLD,13)); fm=g2.getFontMetrics();
        String restart;
        if (mpState == MpState.WAITING_RESTART) restart = "Warte auf anderen Spieler...";
        else restart = "R = Neustart (beide Spieler müssen bestätigen)";
        g2.drawString(restart, bx+(bw-fm.stringWidth(restart))/2, boxY+116);
    }

    private void layoutSettingsPanel() {
        int boardY=getOffsetY(), boardH=getBoardHeight(), sideW=getOffsetX();
        if (sideW<80) return;
        int pw=getWidth(), ph=boardH;
        if (settingsPanel.getBounds().width!=pw || settingsPanel.getBounds().height!=ph
                || settingsPanel.getBounds().y!=boardY)
            settingsPanel.setBounds(0, boardY, pw, ph);
    }

    private float getMoveProgress() {
        return Math.max(0f, Math.min((System.currentTimeMillis()-lastMoveTime)/(float)moveDelayMs, 1f));
    }

    // ── Lobby-Overlay ─────────────────────────────────────────────────────────
    private void drawLobbyOverlay(Graphics2D g2) {
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), bh=getBoardHeight();
        g2.setColor(new Color(0,0,0,60)); g2.fillRect(bx, by, bw, bh);
        int boxW=Math.min(340, bw-40), boxH=140;
        int boxX=bx+(bw-boxW)/2, boxY=by+(bh-boxH)/2;
        g2.setColor(new Color(20,28,22,230)); g2.fillRoundRect(boxX, boxY, boxW, boxH, 28, 28);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(boxX, boxY, boxW, boxH, 28, 28);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fm=g2.getFontMetrics(); String title="SNAKE";
        g2.drawString(title, bx+(bw-fm.stringWidth(title))/2, boxY+46);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15)); fm=g2.getFontMetrics();
        String sub="Einstellungen links & rechts wählen,";
        g2.setColor(new Color(200,200,200));
        g2.drawString(sub, bx+(bw-fm.stringWidth(sub))/2, boxY+76);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15)); fm=g2.getFontMetrics();
        String start="dann ENTER oder SPACE zum Starten";
        g2.setColor(new Color(130,230,130));
        g2.drawString(start, bx+(bw-fm.stringWidth(start))/2, boxY+102);
        double pulse = 0.5+0.5*Math.sin(System.currentTimeMillis()/400.0);
        g2.setColor(new Color(100,200,100,(int)(60+80*pulse)));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 28, 28);
    }

    // ── Hintergrund ──────────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g2, double time) {
        g2.setColor(new Color(30,30,40)); g2.fillRect(0,0,getWidth(),getHeight());
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), bh=getBoardHeight();
        boolean frozen = multiplayerMode ? mpEff1.contains("FREEZE") || mpEff2.contains("FREEZE") : freezeActive;
        Color top    = frozen ? new Color(120,190,240) : new Color(155,223,124);
        Color bottom = frozen ? new Color( 70,130,190) : new Color( 86,156, 74);
        g2.setPaint(new GradientPaint(bx,by,top,bx,by+bh,bottom));
        g2.fillRect(bx,by,bw,bh);
        g2.setColor(new Color(255,255,255,20));
        for (int i=0;i<12;i++) {
            int s=90+pseudo(i*11)%180;
            int x=bx+pseudo(i*17)%Math.max(bw+100,1)-50;
            int y=by+pseudo(i*23)%Math.max(bh+100,1)-50;
            g2.fillOval(x,y,s,s);
        }
        g2.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for (int i=0;i<160;i++) {
            int x=bx+pseudo(i*31)%Math.max(bw,1);
            int y=by+pseudo(i*47)%Math.max(bh,1);
            int h=4+pseudo(i*7)%8;
            float sw=(float)Math.sin(time*1.6+i*0.7)*1.8f;
            g2.setColor(new Color(60,125,55,55));
            g2.draw(new QuadCurve2D.Float(x,y,x+sw,y-h/2f,x+sw*1.4f,y-h));
        }
    }
    private int pseudo(int s) { return Math.abs(s*1103515245+12345); }

    // ── Snake Single-Player Rendering (unverändert) ──────────────────────────
    private void drawSnakeWrapped(Graphics2D g2, float progress, double time) {
        int bx=getOffsetX(), by=getOffsetY(), bw=getBoardWidth(), bh=getBoardHeight();
        Shape oldClip = g2.getClip(); g2.setClip(bx, by, bw, bh);
        List<RenderPoint> basePts = getInterpolatedCenters(progress);
        List<RenderPoint> pts = applySlither(basePts, time);
        drawSnakeAtOffset(g2, pts, time, 0, 0);
        RenderPoint headPt = pts.isEmpty() ? null : pts.get(0);
        if (headPt != null) {
            float relX = (headPt.x - bx) / (float)bw;
            float relY = (headPt.y - by) / (float)bh;
            if (relX < 0.25f)  drawSnakeAtOffset(g2, pts, time,  bw, 0);
            if (relX > 0.75f)  drawSnakeAtOffset(g2, pts, time, -bw, 0);
            if (relY < 0.25f)  drawSnakeAtOffset(g2, pts, time, 0,  bh);
            if (relY > 0.75f)  drawSnakeAtOffset(g2, pts, time, 0, -bh);
            if (relX < 0.25f && relY < 0.25f) drawSnakeAtOffset(g2, pts, time,  bw,  bh);
            if (relX > 0.75f && relY < 0.25f) drawSnakeAtOffset(g2, pts, time, -bw,  bh);
            if (relX < 0.25f && relY > 0.75f) drawSnakeAtOffset(g2, pts, time,  bw, -bh);
            if (relX > 0.75f && relY > 0.75f) drawSnakeAtOffset(g2, pts, time, -bw, -bh);
        }
        g2.setClip(oldClip);
    }

    private List<RenderPoint> getInterpolatedCenters(float progress) {
        int ts=getTileSize(), bx=getOffsetX(), by=getOffsetY();
        List<RenderPoint> pts = new ArrayList<>();
        for (SnakeSegment s : snake) {
            float gx = lerp(s.prevGridX, s.gridX, progress);
            float gy = lerp(s.prevGridY, s.gridY, progress);
            pts.add(new RenderPoint(bx+gx*ts+ts/2f, by+gy*ts+ts/2f));
        }
        return pts;
    }

    private List<RenderPoint> applySlither(List<RenderPoint> base, double time) {
        List<RenderPoint> out = new ArrayList<>();
        for (int i=0; i<base.size(); i++) {
            RenderPoint p=base.get(i);
            if (i==0) { out.add(p); continue; }
            RenderPoint prev = base.get(i-1);
            double ddx=p.x-prev.x, ddy=p.y-prev.y, len=Math.hypot(ddx,ddy);
            double nx=0, ny=0;
            if (len>0.0001) { nx=-ddy/len; ny=ddx/len; }
            double amp  = Math.max(0.0, 3.8-i*0.33);
            double wave = Math.sin(time*8.5-i*0.8)*amp;
            out.add(new RenderPoint((float)(p.x+nx*wave), (float)(p.y+ny*wave)));
        }
        return out;
    }

    private void drawSnakeAtOffset(Graphics2D g2, List<RenderPoint> pts, double time, int offX, int offY) {
        if (pts.isEmpty()) return;
        List<RenderPoint> shifted = new ArrayList<>();
        for (RenderPoint p : pts) shifted.add(new RenderPoint(p.x+offX, p.y+offY));
        int t = getTileSize();
        Path2D.Float path = buildSmoothPath(shifted);
        AffineTransform old = g2.getTransform();
        g2.translate(Math.max(2,t*0.15), Math.max(3,t*0.2));
        g2.setColor(new Color(0,0,0,50));
        g2.setStroke(new BasicStroke(Math.max(8f,t*0.95f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path); g2.setTransform(old);
        SnakeColor c=col();
        if (shieldActive) {
            g2.setColor(new Color(255,215,0,60));
            g2.setStroke(new BasicStroke(Math.max(10f,t*1.1f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }
        if (speedBoost) {
            g2.setColor(new Color(255,255,0,40));
            g2.setStroke(new BasicStroke(Math.max(10f,t*1.05f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }
        g2.setColor(c.outer);
        g2.setStroke(new BasicStroke(Math.max(7f,t*0.85f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path);
        g2.setColor(c.inner);
        g2.setStroke(new BasicStroke(Math.max(4f,t*0.5f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(path);
        for (int i=1; i<shifted.size(); i++) {
            RenderPoint p=shifted.get(i);
            float r=Math.max(2.8f,t*0.33f-i*0.08f);
            g2.setColor(new Color(255,255,255,25));
            g2.fill(new Ellipse2D.Float(p.x-r*0.8f,p.y-r*0.95f,r*1.5f,r*0.75f));
            if (i%2==0) {
                g2.setColor(new Color(23,80,35,65));
                g2.fill(new Ellipse2D.Float(p.x-r*0.65f,p.y-r*0.1f,r*1.25f,r*0.85f));
            }
        }
        drawSnakeHead(g2, shifted, time);
    }

    private Path2D.Float buildSmoothPath(List<RenderPoint> pts) {
        Path2D.Float path=new Path2D.Float();
        if (pts.isEmpty()) return path;
        if (pts.size()==1) { path.moveTo(pts.get(0).x, pts.get(0).y); return path; }
        path.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i=1; i<pts.size()-1; i++) {
            RenderPoint c=pts.get(i), n=pts.get(i+1);
            path.quadTo(c.x,c.y,(c.x+n.x)/2f,(c.y+n.y)/2f);
        }
        path.lineTo(pts.get(pts.size()-1).x, pts.get(pts.size()-1).y);
        return path;
    }

    private float lerp(int a, int b, float t) { return a+(b-a)*t; }
    private SnakeColor col() { return SNAKE_COLORS[selectedColorIndex]; }

    private void drawSnakeHead(Graphics2D g2, List<RenderPoint> pts, double time) {
        if (pts.isEmpty()) return;
        RenderPoint head=pts.get(0);
        int t=getTileSize();
        float headW=Math.max(14f,t*1.2f), headH=Math.max(10f,t*0.95f);
        AffineTransform old=g2.getTransform();
        g2.translate(head.x, head.y);
        g2.rotate(currentDirection.angle());
        SnakeColor c=col();
        g2.setColor(new Color(0,0,0,40));
        g2.fill(new Ellipse2D.Float(-headW/2f+2,-headH/2f+3,headW,headH));
        if (shieldActive) {
            g2.setColor(new Color(255,215,0,90));
            g2.fill(new Ellipse2D.Float(-headW/2f-3,-headH/2f-3,headW+6,headH+6));
        }
        g2.setColor(c.outer);
        g2.fill(new Ellipse2D.Float(-headW/2f,-headH/2f,headW,headH));
        g2.setColor(c.head);
        g2.fill(new Ellipse2D.Float(-headW/2f+2,-headH/2f+2,headW-4,headH-4));
        g2.setColor(new Color(255,255,255,45));
        g2.fill(new Ellipse2D.Float(-3,-6,8,4));
        g2.setColor(new Color(c.head.getRed(),c.head.getGreen(),c.head.getBlue(),170));
        g2.fill(new Ellipse2D.Float(3,-4,7,8));
        drawEyes(g2, time, t);
        drawTongue(g2, time, t);
        g2.setTransform(old);
    }

    private void drawEyes(Graphics2D g2, double time, int t) {
        boolean blink=isBlinking(time);
        float lx=t*0.12f,ly=-t*0.26f,rx=t*0.12f,ry=t*0.26f;
        if (blink) {
            g2.setColor(new Color(20,60,20));
            g2.setStroke(new BasicStroke(Math.max(1.2f,t*0.1f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(lx-2,ly,lx+2,ly));
            g2.draw(new Line2D.Float(rx-2,ry,rx+2,ry)); return;
        }
        float es=Math.max(3.2f,t*0.24f), ps=Math.max(1.4f,t*0.1f);
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(lx-es/2f,ly-es/2f,es,es));
        g2.fill(new Ellipse2D.Float(rx-es/2f,ry-es/2f,es,es));
        g2.setColor(Color.BLACK);
        g2.fill(new Ellipse2D.Float(lx-ps/2f,ly-ps/2f,ps,ps));
        g2.fill(new Ellipse2D.Float(rx-ps/2f,ry-ps/2f,ps,ps));
    }
    private boolean isBlinking(double t) { double c=t%3.4; return(c>0&&c<0.08)||(c>0.15&&c<0.22); }

    private void drawTongue(Graphics2D g2, double time, int t) {
        if (!isTongueOut(time)||gameState!=GameState.RUNNING) return;
        double ph=(time%2.8)/2.8;
        float bx=t*0.55f, len=t*0.45f+(float)Math.sin(ph*Math.PI)*t*0.28f;
        Path2D.Float tongue=new Path2D.Float();
        tongue.moveTo(bx,0); tongue.lineTo(bx+len,-1.3f); tongue.lineTo(bx+len+t*0.22f,-4f);
        tongue.moveTo(bx+len,-1.3f); tongue.lineTo(bx+len+t*0.22f,1.4f);
        g2.setColor(new Color(221,65,96));
        g2.setStroke(new BasicStroke(Math.max(1.2f,t*0.09f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.draw(tongue);
    }
    private boolean isTongueOut(double t) { double c=t%2.8; return c>0.10&&c<0.34; }

    // ── HUD Single-Player ─────────────────────────────────────────────────────
    private void drawHud(Graphics2D g2) {
        int bx=getOffsetX(),by=getOffsetY(),bw=getBoardWidth(),t=getTileSize();
        int hudW=Math.max(145,t*8), hudH=Math.max(38,t*2);
        g2.setColor(new Color(20,28,20,145)); g2.fillRoundRect(bx+12,by+12,hudW,hudH,18,18);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(16,t)));
        g2.drawString("Score: "+score, bx+28, by+12+hudH/2+6);
        if (chaosMode) {
            String badge="CHAOS"; g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(12,t*3/4)));
            FontMetrics fm=g2.getFontMetrics();
            int bw2=fm.stringWidth(badge)+16, bh2=Math.max(24,t+4);
            int bxr=bx+bw-bw2-12, byr=by+12;
            g2.setColor(new Color(180,50,200,160)); g2.fillRoundRect(bxr,byr,bw2,bh2,12,12);
            g2.setColor(Color.WHITE); g2.drawString(badge,bxr+8,byr+bh2/2+5);
        }
        g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(11,t*2/3)));
        g2.setColor(new Color(255,255,255,130));
        g2.drawString("SPACE pause  •  R restart",bx+12,by+getBoardHeight()-8);
    }

    private void drawCenteredOverlay(Graphics2D g2, String title, String subtitle) {
        int bx=getOffsetX(),by=getOffsetY(),bw=getBoardWidth(),bh=getBoardHeight();
        g2.setColor(new Color(0,0,0,130)); g2.fillRect(bx,by,bw,bh);
        int boxW=Math.min(340,bw-40),boxH=140,boxX=bx+(bw-boxW)/2,boxY=by+(bh-boxH)/2;
        g2.setColor(new Color(28,36,28,230)); g2.fillRoundRect(boxX,boxY,boxW,boxH,28,28);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(boxX,boxY,boxW,boxH,28,28);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,30));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(title, bx+(bw-fm.stringWidth(title))/2, boxY+48);
        g2.setFont(new Font("SansSerif",Font.PLAIN,14)); fm=g2.getFontMetrics();
        g2.setColor(new Color(200,200,200));
        g2.drawString(subtitle, bx+(bw-fm.stringWidth(subtitle))/2, boxY+78);
        g2.setFont(new Font("SansSerif",Font.PLAIN,12)); fm=g2.getFontMetrics();
        String hint="Einstellungen links & rechts verfügbar";
        g2.setColor(new Color(150,200,150));
        g2.drawString(hint, bx+(bw-fm.stringWidth(hint))/2, boxY+106);
    }

    // ── Effekt-Overlays Single-Player ─────────────────────────────────────────
    private void drawEffectOverlays(Graphics2D g2) {
        long now=System.currentTimeMillis();
        int x=getOffsetX(), y=getOffsetY()+getBoardHeight()+6;
        int barW=120, barH=16, gap=8;
        g2.setFont(new Font("SansSerif",Font.BOLD,11));
        if (shieldActive)    { drawBar(g2,x,y,barW,barH,new Color(255,215,0),  "Schutz",    (shieldEndTime   -now)/10_000f); x+=barW+gap; }
        if (controlsFlipped) { drawBar(g2,x,y,barW,barH,new Color(160,50,200), "Invertiert",(controlsEndTime -now)/ 8_000f); x+=barW+gap; }
        if (speedBoost)      { drawBar(g2,x,y,barW,barH,new Color(255,220,0),  "Speed",     (speedEndTime    -now)/ 6_000f); x+=barW+gap; }
        if (freezeActive)    { drawBar(g2,x,y,barW,barH,new Color(100,200,255),"Freeze",    (freezeEndTime   -now)/ 5_000f); }
    }

    private void drawBar(Graphics2D g2,int x,int y,int w,int h,Color c,String lbl,float rem) {
        g2.setColor(new Color(0,0,0,100)); g2.fillRoundRect(x,y,w,h,8,8);
        g2.setColor(c); g2.fillRoundRect(x,y,(int)(w*Math.max(0,rem)),h,8,8);
        g2.setColor(Color.WHITE);
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(lbl, x+(w-fm.stringWidth(lbl))/2, y+h-3);
    }

    // ── Futter Single-Player ──────────────────────────────────────────────────
    private void drawFood(Graphics2D g2, double time) {
        if (foodGrid == null) return;
        int t=getTileSize(), bx=getOffsetX(), by=getOffsetY();
        int x=bx+foodGrid.x*t, y=by+foodGrid.y*t;
        float bob=(float)Math.sin(time*3.0+foodGrid.x*0.7+foodGrid.y*0.4)*Math.max(1f,t*0.08f);
        Graphics2D gf=(Graphics2D)g2.create(); gf.translate(0,bob);
        gf.setColor(new Color(0,0,0,35));
        gf.fillOval(x+t/4,y+(int)(t*0.72),t*2/3,Math.max(3,t/5));
        switch (currentFruit) {
            case APPLE -> drawApple(gf,x,y,t); case PEAR -> drawPear(gf,x,y,t);
            case ORANGE -> drawOrange(gf,x,y,t); case GRAPES -> drawGrapes(gf,x,y,t);
            case CHERRY -> drawCherry(gf,x,y,t); case GOLD_APPLE -> drawGoldApple(gf,x,y,t,time);
            case ROTTEN_APPLE -> drawRottenApple(gf,x,y,t,time); case ROTTEN_MEAT -> drawRottenMeat(gf,x,y,t,time);
            case LIGHTNING -> drawLightning(gf,x,y,t,time); case ICE -> drawIceFruit(gf,x,y,t,time);
        }
        gf.dispose();
    }

    private void drawApple(Graphics2D g,int x,int y,int t){
        g.setColor(new Color(216,50,50)); g.fillOval(x+t/5,y+t/4,t/2,t/2); g.fillOval(x+t/2,y+t/4,t/2,t/2);
        g.setColor(new Color(100,60,30)); g.fillRect(x+t/2-1,y+t/10,Math.max(2,t/10),t/4);
        g.setColor(new Color(70,180,75)); g.fillOval(x+t/2+t/8,y+t/10,t/3,t/5);
    }
    private void drawPear(Graphics2D g,int x,int y,int t){
        g.setColor(new Color(184,214,77)); g.fillOval(x+t/3,y+t/6,t/3,t/3); g.fillOval(x+t/5,y+t/2-t/10,t*3/5,t/2);
        g.setColor(new Color(100,60,30)); g.fillRect(x+t/2-1,y+t/12,Math.max(2,t/10),t/4);
        g.setColor(new Color(70,180,75)); g.fillOval(x+t/2+t/8,y+t/12,t/4,t/6);
    }
    private void drawOrange(Graphics2D g,int x,int y,int t){
        g.setColor(new Color(255,151,41)); g.fillOval(x+t/5,y+t/5,t*3/5,t*3/5);
        g.setColor(new Color(255,210,120,90)); g.fillOval(x+t/2-t/8,y+t/3,t/4,t/4);
        g.setColor(new Color(70,180,75)); g.fillOval(x+t/2,y+t/10,t/4,t/6);
    }
    private void drawGrapes(Graphics2D g,int x,int y,int t){
        int gr=Math.max(4,t/4); g.setColor(new Color(120,60,180));
        g.fillOval(x+t/3,y+t/5,gr,gr); g.fillOval(x+t/5,y+t/2-t/12,gr,gr);
        g.fillOval(x+t/2+t/10,y+t/2-t/12,gr,gr); g.fillOval(x+t/3,y+t*2/3-t/10,gr,gr);
        g.setColor(new Color(70,180,75)); g.fillOval(x+t/2,y+t/10,t/4,t/6);
    }
    private void drawCherry(Graphics2D g,int x,int y,int t){
        int cs=Math.max(5,t/3); g.setColor(new Color(180,20,20));
        g.fillOval(x+t/5,y+t/2,cs,cs); g.fillOval(x+t/2,y+t/2,cs,cs);
        g.setColor(new Color(70,180,75)); g.drawLine(x+t/3,y+t/2,x+t/2,y+t/5); g.drawLine(x+t*2/3,y+t/2,x+t/2,y+t/5);
    }
    private void drawGoldApple(Graphics2D g,int x,int y,int t,double time){
        float pulse=1f+(float)Math.sin(time*5)*0.08f; int cx=x+t/2,cy=y+t/2,r=(int)(t*0.38f*pulse);
        g.setColor(new Color(255,215,0,50)); g.fillOval(cx-r-4,cy-r-4,(r+4)*2,(r+4)*2);
        g.setColor(new Color(255,200,0)); g.fillOval(cx-r,cy-r,r*2,r*2);
        g.setColor(new Color(255,240,120,180)); g.fillOval(cx-r/2,cy-r*3/4,r*3/4,r/2);
        g.setColor(new Color(100,60,10)); g.setStroke(new BasicStroke(Math.max(1.5f,t*0.07f)));
        g.drawLine(cx,cy-r,cx+t/6,cy-r-t/5);
        g.setColor(new Color(80,180,50)); g.fillOval(cx+t/10,cy-r-t/4,t/5,t/8);
        int gs=Math.max(2,t/8); g.setColor(new Color(255,255,180,200)); g.fillOval(cx+r/2-gs/2,cy-r/2-gs/2,gs,gs);
    }
    private void drawRottenApple(Graphics2D g,int x,int y,int t,double time){
        int cx=x+t/2,cy=y+t/2,r=(int)(t*0.35f); float w=(float)Math.sin(time*4)*2;
        g.setColor(new Color(110,20,140)); g.fillOval(cx-r+(int)w,cy-r,r*2,r*2);
        g.setColor(new Color(60,10,80)); g.fillOval(cx-r/3,cy-r/4,r/2,r/3); g.fillOval(cx+r/4,cy+r/4,r/3,r/4);
        g.setColor(new Color(180,0,220,40)); g.fillOval(cx-r-3,cy-r-3,(r+3)*2,(r+3)*2);
        g.setColor(new Color(60,30,0)); g.setStroke(new BasicStroke(Math.max(1.5f,t*0.07f)));
        g.drawLine(cx,cy-r,cx-t/6,cy-r-t/5);
        g.setColor(new Color(255,100,0,200)); g.setFont(new Font("SansSerif",Font.BOLD,Math.max(8,t/3)));
        g.drawString("!",cx+r/2,cy-r/2);
    }
    private void drawRottenMeat(Graphics2D g,int x,int y,int t,double time){
        int cx=x+t/2,cy=y+t/2;
        g.setColor(new Color(100,55,30)); g.fillOval(x+t/6,y+t/4,t*2/3,t/2);
        g.setColor(new Color(80,160,50,160));
        g.fillOval(x+t/4,y+t/3,t/5,t/5); g.fillOval(x+t/2,y+t/2-t/8,t/6,t/6); g.fillOval(x+t/3,y+t*2/3-t/8,t/7,t/7);
        double fa=time*4; int fx=cx+(int)(Math.cos(fa)*t*0.38),fy=cy+(int)(Math.sin(fa)*t*0.25)-t/4;
        g.setColor(new Color(20,20,20)); g.fillOval(fx-1,fy-1,3,3);
        g.setColor(new Color(200,200,255,120)); g.fillOval(fx,fy-2,4,2); g.fillOval(fx-3,fy-2,4,2);
        g.setColor(new Color(255,60,60,200)); g.setFont(new Font("SansSerif",Font.BOLD,Math.max(8,t/3)));
        g.drawString("-3",x+t/4,y+t/5+4);
    }
    private void drawLightning(Graphics2D g,int x,int y,int t,double time){
        int cx=x+t/2,cy=y+t/2; float fl=(float)(0.7+Math.sin(time*20)*0.3);
        g.setColor(new Color(255,255,0,(int)(80*fl))); g.fillOval(cx-t/2+2,cy-t/2+2,t-4,t-4);
        Path2D.Float bolt=new Path2D.Float();
        bolt.moveTo(cx+t/10,cy-t/3); bolt.lineTo(cx-t/12,cy-t/14); bolt.lineTo(cx+t/14,cy-t/14);
        bolt.lineTo(cx-t/10,cy+t/3); bolt.lineTo(cx+t/12,cy+t/14); bolt.lineTo(cx-t/14,cy+t/14);
        bolt.closePath();
        g.setColor(new Color(255,235,0,(int)(255*fl))); g.fill(bolt);
        g.setColor(new Color(255,255,180,(int)(200*fl))); g.setStroke(new BasicStroke(1f)); g.draw(bolt);
    }
    private void drawIceFruit(Graphics2D g,int x,int y,int t,double time){
        int cx=x+t/2,cy=y+t/2,r=(int)(t*0.36f); float sp=(float)(0.5+Math.sin(time*6)*0.5);
        g.setColor(new Color(150,220,255,200)); g.fillOval(cx-r,cy-r,r*2,r*2);
        g.setColor(new Color(200,240,255,200)); g.setStroke(new BasicStroke(Math.max(1f,t*0.07f)));
        for (int i=0;i<6;i++){double a=i*Math.PI/3; g.draw(new Line2D.Float(cx,cy,cx+(float)(Math.cos(a)*r*0.85f),cy+(float)(Math.sin(a)*r*0.85f)));}
        g.setColor(new Color(255,255,255,(int)(180*sp))); g.fillOval(cx-r/2,cy-r*2/3,r*2/3,r/3);
        int gs=Math.max(2,t/10); g.setColor(new Color(255,255,255,(int)(220*sp)));
        g.fillOval(cx+r/2-gs/2,cy-r/2-gs/2,gs,gs); g.fillOval(cx-r*2/3,cy+r/4,gs,gs);
    }

    // ===== Input =====
    @Override
    public void keyPressed(KeyEvent e) {
        if (multiplayerMode) {
            handleMpKey(e);
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> {
                if (gameState==GameState.RUNNING && currentDirection!=Direction.DOWN) nextDirection=Direction.UP;
            }
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> {
                if (gameState==GameState.RUNNING && currentDirection!=Direction.UP)   nextDirection=Direction.DOWN;
            }
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> {
                if (gameState==GameState.RUNNING && currentDirection!=Direction.RIGHT) nextDirection=Direction.LEFT;
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (gameState==GameState.RUNNING && currentDirection!=Direction.LEFT)  nextDirection=Direction.RIGHT;
            }
            case KeyEvent.VK_SPACE -> {
                if (gameState==GameState.LOBBY)        startGame();
                else if (gameState==GameState.RUNNING) { gameState=GameState.PAUSED; }
                else if (gameState==GameState.PAUSED)  { gameState=GameState.RUNNING; }
            }
            case KeyEvent.VK_ENTER -> {
                if (gameState==GameState.LOBBY || gameState==GameState.GAME_OVER) startGame();
            }
            case KeyEvent.VK_R -> startGame();
        }
    }

    private void handleMpKey(KeyEvent e) {
        if (mpOut == null) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> mpOut.println("DIR:UP");
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> mpOut.println("DIR:DOWN");
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> mpOut.println("DIR:LEFT");
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> mpOut.println("DIR:RIGHT");
            case KeyEvent.VK_R -> {
                if (mpState == MpState.GAME_OVER) mpOut.println("RESTART");
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}

    // ===== Getter/Setter =====
    public int  getSelectedColorIndex()      { return selectedColorIndex; }
    public void setSelectedColorIndex(int i) { selectedColorIndex=i; repaint(); }

    /** Setzt Multiplayer-Spieler-ID (1 oder 2) – wird vom Wrapper gesetzt. */
    public void setMyPlayerId(int id) { this.myPlayerId = id; }

    // ===== Main (Single-Player Test) =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Modern Snake");
            SnakeGameNew g = new SnakeGameNew();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(true); f.add(g); f.pack();
            f.setLocationRelativeTo(null); f.setVisible(true);
            g.requestFocusInWindow();
        });
    }
}


// =============================================================================
//  SETTINGS PANEL
// =============================================================================
class SettingsPanel extends JPanel {

    private final SnakeGameNew game;
    private int     previewColorIndex = 0;
    private boolean chaosMode         = false;

    private static final int CARD_W  = 170;
    private static final int CARD_H  = 220;
    private static final int PADDING = 16;

    SettingsPanel(SnakeGameNew game) {
        this.game=game; setOpaque(false); setLayout(null);
        previewColorIndex=game.getSelectedColorIndex();
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY()); game.requestFocusInWindow();
            }
        });
    }

    public boolean isChaosMode() { return chaosMode; }

    private void handleClick(int mx, int my) {
        Rectangle lc=getLeftCardBounds();
        if (lc!=null) {
            Rectangle aL=new Rectangle(lc.x+8,           lc.y+lc.height/2-15, 24, 30);
            Rectangle aR=new Rectangle(lc.x+lc.width-32, lc.y+lc.height/2-15, 24, 30);
            if (aL.contains(mx,my)) {
                previewColorIndex=(previewColorIndex-1+SnakeGameNew.SNAKE_COLORS.length)%SnakeGameNew.SNAKE_COLORS.length;
                game.setSelectedColorIndex(previewColorIndex); repaint();
            } else if (aR.contains(mx,my)) {
                previewColorIndex=(previewColorIndex+1)%SnakeGameNew.SNAKE_COLORS.length;
                game.setSelectedColorIndex(previewColorIndex); repaint();
            }
        }
        Rectangle rc=getRightCardBounds();
        if (rc!=null) {
            int bW=rc.width-24, bX=rc.x+12;
            if (new Rectangle(bX,rc.y+90, bW,36).contains(mx,my))  { chaosMode=false; repaint(); }
            if (new Rectangle(bX,rc.y+134,bW,36).contains(mx,my))  { chaosMode=true;  repaint(); }
        }
    }

    private Rectangle getLeftCardBounds() {
        int bx=getBoardX();
        if (bx < CARD_W+PADDING*2) return null;
        return new Rectangle((bx-CARD_W)/2, (getHeight()-CARD_H)/2, CARD_W, CARD_H);
    }
    private Rectangle getRightCardBounds() {
        int bx=getBoardX(), bw=getBoardWidth(), ra=getWidth()-bx-bw;
        if (ra < CARD_W+PADDING*2) return null;
        return new Rectangle(bx+bw+(ra-CARD_W)/2, (getHeight()-CARD_H)/2, CARD_W, CARD_H);
    }
    private int getBoardX() {
        int ts=Math.max(1,Math.min(getWidth()/SnakeGameNew.GRID_WIDTH,getHeight()/SnakeGameNew.GRID_HEIGHT));
        return (getWidth()-ts*SnakeGameNew.GRID_WIDTH)/2;
    }
    private int getBoardWidth() {
        int ts=Math.max(1,Math.min(getWidth()/SnakeGameNew.GRID_WIDTH,getHeight()/SnakeGameNew.GRID_HEIGHT));
        return ts*SnakeGameNew.GRID_WIDTH;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawColorCard(g2); drawModeCard(g2); g2.dispose();
    }

    private void drawColorCard(Graphics2D g2) {
        Rectangle r=getLeftCardBounds(); if(r==null)return;
        g2.setColor(new Color(20,24,32,210)); g2.fillRoundRect(r.x,r.y,r.width,r.height,18,18);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(r.x,r.y,r.width,r.height,18,18);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,13));
        FontMetrics fm=g2.getFontMetrics(); String title="Farbe";
        g2.drawString(title, r.x+(r.width-fm.stringWidth(title))/2, r.y+24);
        SnakeGameNew.SnakeColor col=SnakeGameNew.SNAKE_COLORS[previewColorIndex];
        g2.setFont(new Font("SansSerif",Font.PLAIN,12)); fm=g2.getFontMetrics();
        g2.setColor(new Color(200,200,200));
        g2.drawString(col.name, r.x+(r.width-fm.stringWidth(col.name))/2, r.y+42);
        drawSnakePreview(g2,r,col); drawColorDots(g2,r);
        drawArrow(g2, r.x+8,          r.y+r.height/2-12, false);
        drawArrow(g2, r.x+r.width-30, r.y+r.height/2-12, true);
    }

    private void drawSnakePreview(Graphics2D g2, Rectangle r, SnakeGameNew.SnakeColor col) {
        int cx=r.x+r.width/2, cy=r.y+110, ss=10;
        int[][] o={{0,0},{-14,0},{-22,-10},{-22,-22},{-14,-32},{0,-32},{10,-22}};
        for (int[] p:o) { g2.setColor(new Color(0,0,0,50)); g2.fillOval(cx+p[0]-ss/2+2,cy+p[1]-ss/2+2,ss,ss); }
        for (int i=o.length-1; i>=1; i--) {
            g2.setColor(col.outer); g2.fillOval(cx+o[i][0]-ss/2,  cy+o[i][1]-ss/2,  ss,  ss);
            g2.setColor(col.inner); g2.fillOval(cx+o[i][0]-ss/2+2,cy+o[i][1]-ss/2+2,ss-4,ss-4);
        }
        int hs=ss+4;
        g2.setColor(col.outer); g2.fillOval(cx+o[0][0]-hs/2,  cy+o[0][1]-hs/2,  hs,  hs);
        g2.setColor(col.head);  g2.fillOval(cx+o[0][0]-hs/2+2,cy+o[0][1]-hs/2+2,hs-4,hs-4);
        g2.setColor(Color.WHITE); g2.fillOval(cx+o[0][0]+1,cy+o[0][1]-3,3,3);
        g2.setColor(Color.BLACK); g2.fillOval(cx+o[0][0]+2,cy+o[0][1]-2,2,2);
    }

    private void drawColorDots(Graphics2D g2, Rectangle r) {
        int total=SnakeGameNew.SNAKE_COLORS.length, sp=(r.width-20)/total;
        int sx=r.x+10+sp/2, dotY=r.y+r.height-28;
        for (int i=0; i<total; i++) {
            SnakeGameNew.SnakeColor c=SnakeGameNew.SNAKE_COLORS[i]; int dx=sx+i*sp;
            if (i==previewColorIndex) {
                g2.setColor(Color.WHITE); g2.fillOval(dx-7,dotY-7,14,14);
                g2.setColor(c.inner);     g2.fillOval(dx-5,dotY-5,10,10);
            } else {
                g2.setColor(new Color(c.inner.getRed(),c.inner.getGreen(),c.inner.getBlue(),160));
                g2.fillOval(dx-4,dotY-4,8,8);
            }
        }
    }

    private void drawModeCard(Graphics2D g2) {
        Rectangle r=getRightCardBounds(); if(r==null)return;
        g2.setColor(new Color(20,24,32,210)); g2.fillRoundRect(r.x,r.y,r.width,r.height,18,18);
        g2.setColor(new Color(255,255,255,30)); g2.drawRoundRect(r.x,r.y,r.width,r.height,18,18);
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif",Font.BOLD,13));
        FontMetrics fm=g2.getFontMetrics(); String title="Spielmodus";
        g2.drawString(title, r.x+(r.width-fm.stringWidth(title))/2, r.y+24);
        g2.setFont(new Font("SansSerif",Font.PLAIN,10)); g2.setColor(new Color(160,160,160));
        String desc=chaosMode?"Spezialfrüchte aktiv!":"Klassisches Snake"; fm=g2.getFontMetrics();
        g2.drawString(desc, r.x+(r.width-fm.stringWidth(desc))/2, r.y+40);
        drawModeIcon(g2,r);
        drawModeButton(g2, r.x+12, r.y+90,  r.width-24, 36, "Normal", !chaosMode, new Color(50,160,80));
        drawModeButton(g2, r.x+12, r.y+134, r.width-24, 36, "Chaos",   chaosMode, new Color(160,50,200));
        g2.setFont(new Font("SansSerif",Font.PLAIN,9)); g2.setColor(new Color(120,120,120));
        String hint="Gilt ab nächstem Start"; fm=g2.getFontMetrics();
        g2.drawString(hint, r.x+(r.width-fm.stringWidth(hint))/2, r.y+r.height-10);
    }

    private void drawModeIcon(Graphics2D g2, Rectangle r) {
        int cx=r.x+r.width/2, cy=r.y+68;
        if (!chaosMode) {
            g2.setColor(new Color(216,50,50)); g2.fillOval(cx-10,cy-8,12,12); g2.fillOval(cx-2,cy-8,12,12);
            g2.setColor(new Color(100,60,30)); g2.fillRect(cx+2,cy-13,2,6);
            g2.setColor(new Color(70,180,75)); g2.fillOval(cx+4,cy-13,7,5);
        } else {
            g2.setColor(new Color(255,215,0)); g2.fillOval(cx-20,cy-8,12,12);
            g2.setColor(new Color(255,220,0));
            int[]bxp={cx-2,cx+2,cx,cx+4,cx-2,cx+2},byp={cy-8,cy-1,cy-1,cy-1,cy+6,cy-1};
            g2.fillPolygon(bxp,byp,6);
            g2.setColor(new Color(100,200,255)); g2.fillOval(cx+8,cy-8,12,12);
        }
    }

    private void drawModeButton(Graphics2D g2,int x,int y,int w,int h,String lbl,boolean sel,Color acc) {
        if (sel) {
            g2.setColor(new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),200));
            g2.fillRoundRect(x,y,w,h,10,10);
            g2.setColor(acc.brighter()); g2.drawRoundRect(x,y,w,h,10,10);
        } else {
            g2.setColor(new Color(50,55,65,180)); g2.fillRoundRect(x,y,w,h,10,10);
            g2.setColor(new Color(100,100,110));  g2.drawRoundRect(x,y,w,h,10,10);
        }
        g2.setColor(sel ? Color.WHITE : new Color(160,160,160));
        g2.setFont(new Font("SansSerif",Font.BOLD,12));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(lbl, x+(w-fm.stringWidth(lbl))/2, y+h/2+5);
    }

    private void drawArrow(Graphics2D g2, int x, int y, boolean right) {
        g2.setColor(new Color(255,255,255,60)); g2.fillRoundRect(x,y,22,24,6,6);
        g2.setColor(new Color(255,255,255,180));
        int[] px, py;
        if (right) { px=new int[]{x+6,x+16,x+6};  py=new int[]{y+5,y+12,y+19}; }
        else       { px=new int[]{x+16,x+6,x+16}; py=new int[]{y+5,y+12,y+19}; }
        g2.fillPolygon(px,py,3);
    }
}