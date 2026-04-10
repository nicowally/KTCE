package gamehub.games.snake_old;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Nokia 3310-style Snake – sieht exakt wie das Original aus:
 *  - LCD-grüner Hintergrund (155/188/15)
 *  - Dicker Pixel-Rahmen ums Spielfeld
 *  - Score oben links in Monospace (wie Nokia-Display)
 *  - Schlange = ausgefüllte schwarze Quadrate
 *  - Futter  = kleiner Pixel-Sprite (wie Nokia)
 *  - Kein Anti-Aliasing, reiner Pixel-Look
 */
public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    // ── Grid ──────────────────────────────────────────────────────────────────
    private static final int GRID_WIDTH  = 21;   // Nokia 3310 hatte ~21×16 Zellen
    private static final int GRID_HEIGHT = 16;
    private static final int GAME_SPEED  = 150;  // ms pro Schritt (Nokia-Tempo)

    // ── Nokia-LCD-Farben ──────────────────────────────────────────────────────
    private static final Color LCD_GREEN  = new Color(155, 188,  15);  // Hintergrund
    private static final Color LCD_DARK   = new Color( 40,  40,  15);  // Pixel-Farbe
    private static final Color LCD_MID    = new Color( 90, 120,  10);  // leicht heller (Rahmen-Schatten)

    // ── Spielzustand ──────────────────────────────────────────────────────────
    private final Timer          timer;
    private final List<Point>    snake    = new ArrayList<>();
    private final Random         random   = new Random();

    private Point     food;
    private Direction direction     = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;

    private boolean running  = true;
    private boolean gameOver = false;
    private boolean paused   = false;
    private int     score    = 0;

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    // ── Konstruktor ───────────────────────────────────────────────────────────
    public SnakeGame() {
        setPreferredSize(new Dimension(480, 400));  // kompaktes Nokia-Display-Feeling
        setBackground(LCD_GREEN);
        setFocusable(true);
        addKeyListener(this);

        startGame();

        timer = new Timer(GAME_SPEED, this);
        timer.start();
    }

    // ── Spielstart ────────────────────────────────────────────────────────────
    private void startGame() {
        snake.clear();
        // Schlange startet mittig mit 4 Segmenten
        int startY = GRID_HEIGHT / 2;
        snake.add(new Point(8, startY));
        snake.add(new Point(7, startY));
        snake.add(new Point(6, startY));
        snake.add(new Point(5, startY));

        direction     = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        running       = true;
        gameOver      = false;
        paused        = false;
        score         = 0;

        spawnFood();
        repaint();
    }

    private void spawnFood() {
        Point candidate;
        do {
            candidate = new Point(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT));
        } while (snake.contains(candidate));
        food = candidate;
    }

    // ── Kachelgröße: immer ganzzahlig, möglichst groß ────────────────────────
    /** Berechnet die Kachelgröße passend zur aktuellen Panel-Größe. */
    private int tileSize() {
        int topBarHeight = 28;   // Platz für Score-Zeile
        int padding      = 10;   // Abstand an allen Seiten
        int availW = getWidth()  - padding * 2;
        int availH = getHeight() - topBarHeight - padding * 2;
        return Math.max(4, Math.min(availW / GRID_WIDTH, availH / GRID_HEIGHT));
    }

    private int boardW()  { return GRID_WIDTH  * tileSize(); }
    private int boardH()  { return GRID_HEIGHT * tileSize(); }
    private int boardX()  { return (getWidth() - boardW()) / 2; }
    private int boardY()  {
        // Score-Zeile + kleiner Abstand
        return 30;
    }

    // ── Game-Loop ─────────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) updateGame();
        repaint();
    }

    private void updateGame() {
        // Richtungswechsel (kein 180°-Turn)
        if (!isOpposite(direction, nextDirection)) direction = nextDirection;

        Point head    = snake.get(0);
        Point newHead = new Point(head);

        switch (direction) {
            case UP    -> newHead.y--;
            case DOWN  -> newHead.y++;
            case LEFT  -> newHead.x--;
            case RIGHT -> newHead.x++;
        }

        // Wand-Kollision
        if (newHead.x < 0 || newHead.x >= GRID_WIDTH ||
                newHead.y < 0 || newHead.y >= GRID_HEIGHT) {
            running = false; gameOver = true; return;
        }

        // Selbst-Kollision (nicht gegen letztes Segment, das gleich wegfällt)
        boolean ateFood = newHead.equals(food);
        int limit = ateFood ? snake.size() : snake.size() - 1;
        for (int i = 1; i < limit; i++) {
            if (snake.get(i).equals(newHead)) {
                running = false; gameOver = true; return;
            }
        }

        snake.add(0, newHead);
        if (ateFood) {
            score++;
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private boolean isOpposite(Direction a, Direction b) {
        return (a == Direction.UP    && b == Direction.DOWN)
                || (a == Direction.DOWN  && b == Direction.UP)
                || (a == Direction.LEFT  && b == Direction.RIGHT)
                || (a == Direction.RIGHT && b == Direction.LEFT);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // LCD-Hintergrund
        g.setColor(LCD_GREEN);
        g.fillRect(0, 0, getWidth(), getHeight());

        Graphics2D g2 = (Graphics2D) g;
        // Kein Anti-Aliasing → echter Pixel-Look
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        drawScoreBar(g2);
        drawBorder(g2);
        drawFood(g2);
        drawSnake(g2);

        if (paused   && !gameOver) drawOverlay(g2, "PAUSED",    "SPACE = WEITER");
        if (gameOver)              drawOverlay(g2, "GAME OVER", "R = NEU STARTEN");
    }

    /** Score-Anzeige: Nokia-typisch oben links, Monospace-Font */
    private void drawScoreBar(Graphics2D g) {
        g.setColor(LCD_DARK);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        // Nokia zeigte den Score immer ohne Beschriftung, nur die Zahl
        String scoreStr = String.format("%04d", score);
        g.drawString(scoreStr, boardX(), 20);
    }

    /** Dicker Pixel-Rahmen ums Spielfeld – exakt wie Nokia 3310 */
    private void drawBorder(Graphics2D g) {
        int x = boardX();
        int y = boardY();
        int w = boardW();
        int h = boardH();

        // Äußerer Rahmen (2px dick)
        g.setColor(LCD_DARK);
        for (int t = 0; t < 2; t++) {
            g.drawRect(x - t, y - t, w + t * 2, h + t * 2);
        }
        // Innerer Schatten (1px, etwas heller)
        g.setColor(LCD_MID);
        g.drawRect(x + 1, y + 1, w - 2, h - 2);
    }

    /**
     * Nokia 3310-Schlange: jedes Segment ist ein kleines ausgefülltes Quadrat,
     * das zentriert in seiner Zelle sitzt (~60 % der Tile-Größe).
     * Der Kopf ist etwas größer (~80 %) und hat zwei Augen-Pixel.
     */
    private void drawSnake(Graphics2D g) {
        int t  = tileSize();
        int bx = boardX();
        int by = boardY();

        // Körper-Segmente: 60 % der Tile, zentriert
        int bodySize   = Math.max(2, (t * 6) / 10);
        int bodyOffset = (t - bodySize) / 2;

        g.setColor(LCD_DARK);

        for (int i = snake.size() - 1; i >= 0; i--) {
            Point p = snake.get(i);
            int px = bx + p.x * t;
            int py = by + p.y * t;

            if (i == 0) {
                // Kopf: ~80 % der Tile, zentriert
                int headSize   = Math.max(3, (t * 8) / 10);
                int headOffset = (t - headSize) / 2;
                g.fillRect(px + headOffset, py + headOffset, headSize, headSize);

                // Augen: 2 kleine Pixel-Dots, je nach Richtung positioniert
                int eyeSize = Math.max(1, t / 8);
                drawHeadEyes(g, px, py, t, eyeSize);
            } else {
                g.fillRect(px + bodyOffset, py + bodyOffset, bodySize, bodySize);
            }
        }
    }

    /**
     * Zeichnet zwei Augen-Pixel auf den Kopf, ausgerichtet nach der Bewegungsrichtung.
     */
    private void drawHeadEyes(Graphics2D g, int px, int py, int t, int es) {
        int mid  = t / 2;
        int near = t / 5;          // Abstand von der Vorderkante
        int side = t / 3;          // seitlicher Abstand zur Mitte

        int e1x, e1y, e2x, e2y;
        switch (direction) {
            case RIGHT -> { e1x = px + t - near - es; e1y = py + mid - side;     e2x = e1x;         e2y = py + mid + side - es; }
            case LEFT  -> { e1x = px + near;           e1y = py + mid - side;     e2x = e1x;         e2y = py + mid + side - es; }
            case UP    -> { e1x = px + mid - side;     e1y = py + near;           e2x = px + mid + side - es; e2y = e1y; }
            default    -> { e1x = px + mid - side;     e1y = py + t - near - es;  e2x = px + mid + side - es; e2y = e1y; }
        }

        // Augen werden in LCD_GREEN gezeichnet (heller Kontrast auf dunklem Kopf)
        g.setColor(LCD_GREEN);
        g.fillRect(e1x, e1y, es, es);
        g.fillRect(e2x, e2y, es, es);
        g.setColor(LCD_DARK);
    }

    /**
     * Futter-Sprite im Nokia-Stil:
     * Bei kleinen Tiles: einfacher Punkt.
     * Bei größeren Tiles: der klassische kleine "Apfel"-Pixel-Sprite.
     */
    private void drawFood(Graphics2D g) {
        int t  = tileSize();
        int bx = boardX();
        int by = boardY();
        int fx = bx + food.x * t;
        int fy = by + food.y * t;

        g.setColor(LCD_DARK);

        if (t <= 8) {
            // Sehr kleine Tiles: nur ein Pixel-Punkt
            int b = Math.max(2, t / 2);
            g.fillRect(fx + t / 2 - b / 2, fy + t / 2 - b / 2, b, b);
        } else {
            // Nokia-typischer Futter-Sprite (Äpfelchen):
            // Pixel-Koordinaten relativ zum 6×6-Raster innerhalb der Tile
            int b = t / 6;  // Block-Einheit
            // Stiel
            g.fillRect(fx + 3 * b, fy + 0 * b, b, b);
            // Blatt
            g.fillRect(fx + 4 * b, fy + 0 * b, b, b);
            // Apfelkörper
            g.fillRect(fx + 2 * b, fy + 1 * b, b, b);
            g.fillRect(fx + 3 * b, fy + 1 * b, b, b);
            g.fillRect(fx + 4 * b, fy + 1 * b, b, b);
            g.fillRect(fx + 1 * b, fy + 2 * b, b, b);
            g.fillRect(fx + 2 * b, fy + 2 * b, b, b);
            g.fillRect(fx + 3 * b, fy + 2 * b, b, b);
            g.fillRect(fx + 4 * b, fy + 2 * b, b, b);
            g.fillRect(fx + 2 * b, fy + 3 * b, b, b);
            g.fillRect(fx + 3 * b, fy + 3 * b, b, b);
        }
    }

    /** Pause / Game-Over Overlay im Nokia-Stil */
    private void drawOverlay(Graphics2D g, String title, String subtitle) {
        int bx = boardX();
        int by = boardY();
        int bw = boardW();
        int bh = boardH();

        // Box zentriert im Spielfeld
        int boxW = Math.min(200, bw - 20);
        int boxH = 60;
        int boxX = bx + (bw - boxW) / 2;
        int boxY = by + (bh - boxH) / 2;

        // LCD-Hintergrund der Box (wirkt wie "ausgebleicht")
        g.setColor(LCD_GREEN);
        g.fillRect(boxX, boxY, boxW, boxH);

        // Rahmen
        g.setColor(LCD_DARK);
        g.drawRect(boxX,     boxY,     boxW,     boxH);
        g.drawRect(boxX + 2, boxY + 2, boxW - 4, boxH - 4);

        // Text
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title,    boxX + (boxW - fm.stringWidth(title))    / 2, boxY + 22);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        fm = g.getFontMetrics();
        g.drawString(subtitle, boxX + (boxW - fm.stringWidth(subtitle)) / 2, boxY + 44);
    }

    // ── Input ──────────────────────────────────────────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> { if (direction != Direction.DOWN)  nextDirection = Direction.UP;    }
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> { if (direction != Direction.UP)    nextDirection = Direction.DOWN;  }
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> { if (direction != Direction.RIGHT) nextDirection = Direction.LEFT;  }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> { if (direction != Direction.LEFT)  nextDirection = Direction.RIGHT; }
            case KeyEvent.VK_SPACE -> { if (!gameOver) paused = !paused; }
            case KeyEvent.VK_R     -> startGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake – Nokia Classic");
            SnakeGame game = new SnakeGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}