package gamehub.games.snake_old;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    private static final int GRID_WIDTH = 24;
    private static final int GRID_HEIGHT = 16;
    private static final int GAME_SPEED = 120;

    private static final Color LCD_BG = new Color(155, 188, 15);
    private static final Color LCD_DARK = new Color(35, 35, 35);

    private final Timer timer;
    private final List<Point> snake = new ArrayList<>();
    private final Random random = new Random();

    private final Set<Point> walls = new HashSet<>();

    private Point food;

    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;

    private boolean running = true;
    private boolean gameOver = false;
    private boolean paused = false;

    private int score = 0;

    // Verdauungsanimation
    private int pendingGrowth = 0;
    private int digestIndex = -1;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public SnakeGame() {
        setPreferredSize(new Dimension(950, 720));
        setBackground(LCD_BG);
        setFocusable(true);
        addKeyListener(this);

        buildWalls(); // jetzt leer
        startGame();

        timer = new Timer(GAME_SPEED, this);
        timer.start();
    }

    private void buildWalls() {
        walls.clear(); // keine Wände
    }

    private void startGame() {
        snake.clear();

        snake.add(new Point(10, 10));
        snake.add(new Point(9, 10));
        snake.add(new Point(8, 10));
        snake.add(new Point(7, 10));

        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;

        running = true;
        gameOver = false;
        paused = false;
        score = 0;

        pendingGrowth = 0;
        digestIndex = -1;

        spawnFood();
        repaint();
    }

    private void spawnFood() {
        Point candidate;
        do {
            candidate = new Point(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT));
        } while (snake.contains(candidate) || walls.contains(candidate));

        food = candidate;
    }

    /**
     * Berechnet die Kachelgröße so, dass sie immer ein Vielfaches von 6 ist.
     * Dadurch liegen die Blöcke innerhalb und zwischen den Zellen nahtlos aneinander.
     */
    private int getTileSize() {
        int availableWidth = getWidth() - 80;
        int availableHeight = getHeight() - 140;
        int rawTile = Math.max(12, Math.min(availableWidth / GRID_WIDTH, availableHeight / GRID_HEIGHT));
        return (rawTile / 6) * 6;
    }

    private int getBoardWidth() {
        return GRID_WIDTH * getTileSize();
    }

    private int getBoardHeight() {
        return GRID_HEIGHT * getTileSize();
    }

    private int getBoardX() {
        return (getWidth() - getBoardWidth()) / 2;
    }

    private int getBoardY() {
        return 90;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        if (isOpposite(direction, nextDirection)) {
            nextDirection = direction;
        }
        direction = nextDirection;

        Point head = snake.get(0);
        Point newHead = new Point(head);

        switch (direction) {
            case UP -> newHead.y--;
            case DOWN -> newHead.y++;
            case LEFT -> newHead.x--;
            case RIGHT -> newHead.x++;
        }

        if (newHead.x < 0 || newHead.x >= GRID_WIDTH || newHead.y < 0 || newHead.y >= GRID_HEIGHT) {
            running = false;
            gameOver = true;
            return;
        }

        if (walls.contains(newHead)) {
            running = false;
            gameOver = true;
            return;
        }

        boolean ateFood = newHead.equals(food);

        snake.add(0, newHead);

        int collisionLimit = ateFood ? snake.size() : snake.size() - 1;
        for (int i = 1; i < collisionLimit; i++) {
            if (snake.get(i).equals(newHead)) {
                running = false;
                gameOver = true;
                return;
            }
        }

        boolean growAtTailThisTick = false;

        if (pendingGrowth > 0) {
            if (digestIndex < 0) {
                digestIndex = 1;
            } else {
                digestIndex++;
            }

            if (digestIndex >= snake.size() - 1) {
                growAtTailThisTick = true;
                pendingGrowth--;
                digestIndex = pendingGrowth > 0 ? 1 : -1;
            }
        }

        if (ateFood) {
            score++;
            pendingGrowth++;
            if (digestIndex < 0) {
                digestIndex = 1;
            }
            spawnFood();
        }

        if (!growAtTailThisTick) {
            snake.remove(snake.size() - 1);
        }
    }

    private boolean isOpposite(Direction current, Direction next) {
        return (current == Direction.UP && next == Direction.DOWN)
                || (current == Direction.DOWN && next == Direction.UP)
                || (current == Direction.LEFT && next == Direction.RIGHT)
                || (current == Direction.RIGHT && next == Direction.LEFT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        drawPhoneScreen(g2);
        drawTopBar(g2);
        drawBoardFrame(g2);
        drawWalls(g2);   // zeichnet nichts, da walls leer
        drawSnake(g2);
        drawFood(g2);

        if (paused && !gameOver) {
            drawCenterMessage(g2, "PAUSE", "SPACE");
        }

        if (gameOver) {
            drawCenterMessage(g2, "GAME OVER", "R = RESTART");
        }

        g2.dispose();
    }

    private void drawPhoneScreen(Graphics2D g) {
        g.setColor(LCD_BG);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawTopBar(Graphics2D g) {
        g.setColor(LCD_DARK);
        g.setFont(new Font("Monospaced", Font.BOLD, 26));

        g.drawString(String.format("%04d", score * 7 + 491), 35, 45);

        drawMiniSnakeIcon(g, getWidth() - 150, 28, 8);

        g.drawString(String.format("%02d", score), getWidth() - 80, 45);

        g.drawLine(25, 60, getWidth() - 25, 60);
    }

    private void drawMiniSnakeIcon(Graphics2D g, int x, int y, int pixel) {
        int[][] icon = {
                {1, 0}, {2, 0},
                {0, 1}, {1, 1}, {2, 1}, {3, 1},
                {1, 2}, {2, 2},
                {2, 3}
        };

        for (int[] p : icon) {
            g.fillRect(x + p[0] * pixel, y + p[1] * pixel, pixel, pixel);
        }
    }

    private void drawBoardFrame(Graphics2D g) {
        int x = getBoardX();
        int y = getBoardY();
        int w = getBoardWidth();
        int h = getBoardHeight();

        g.setColor(LCD_DARK);
        g.drawRect(x, y, w, h);
        g.drawRect(x + 3, y + 3, w - 6, h - 6);
    }

    private void drawWalls(Graphics2D g) {
        // keine Wände
    }

    private void drawSnake(Graphics2D g) {
        int tile = getTileSize();
        int boardX = getBoardX();
        int boardY = getBoardY();

        g.setColor(LCD_DARK);

        for (int i = 0; i < snake.size(); i++) {
            Point current = snake.get(i);
            Point prev = (i > 0) ? snake.get(i - 1) : null;
            Point next = (i < snake.size() - 1) ? snake.get(i + 1) : null;

            boolean fat = (i == digestIndex);

            drawSnakePiece(
                    g,
                    boardX + current.x * tile,
                    boardY + current.y * tile,
                    tile,
                    prev,
                    current,
                    next,
                    fat,
                    i
            );
        }
    }

    /**
     * Zeichnet ein Segment der Schlange mit drei Blöcken in einer Linie.
     * Die Linie wechselt je nach Index zwischen oben (Reihe 1) und unten (Reihe 2),
     * sodass das Muster ---___---___--- entsteht – jetzt ohne freie Zeile dazwischen.
     */
    private void drawSnakePiece(Graphics2D g, int x, int y, int size,
                                Point prev, Point current, Point next, boolean fat, int index) {
        int block = size / 6;

        // Bestimme die Art des Segments
        boolean horizontal = false;
        boolean vertical = false;
        boolean diagonal = false;

        if (prev != null && next != null) {
            if (prev.y == current.y && next.y == current.y) {
                horizontal = true;
            } else if (prev.x == current.x && next.x == current.x) {
                vertical = true;
            } else {
                diagonal = true;
            }
        } else if (prev != null) {
            if (prev.y == current.y) horizontal = true;
            else if (prev.x == current.x) vertical = true;
            else diagonal = true;
        } else if (next != null) {
            if (next.y == current.y) horizontal = true;
            else if (next.x == current.x) vertical = true;
            else diagonal = true;
        } else {
            g.fillRect(x + 2 * block, y + 2 * block, block, block);
            return;
        }

        boolean even = (index % 2 == 0);

        if (horizontal) {
            int row = even ? 1 : 2; // jetzt direkt benachbart
            fillPixel(g, x, y, block, 1, row);
            fillPixel(g, x, y, block, 2, row);
            fillPixel(g, x, y, block, 3, row);
            if (fat) {
                fillPixel(g, x, y, block, 2, 2); // Mitte
            }
        } else if (vertical) {
            int col = even ? 1 : 2;
            fillPixel(g, x, y, block, col, 1);
            fillPixel(g, x, y, block, col, 2);
            fillPixel(g, x, y, block, col, 3);
            if (fat) {
                fillPixel(g, x, y, block, 2, 2);
            }
        } else if (diagonal) {
            if (even) {
                fillPixel(g, x, y, block, 1, 1);
                fillPixel(g, x, y, block, 2, 2);
                fillPixel(g, x, y, block, 3, 3);
            } else {
                fillPixel(g, x, y, block, 3, 1);
                fillPixel(g, x, y, block, 2, 2);
                fillPixel(g, x, y, block, 1, 3);
            }
            if (fat) {
                fillPixel(g, x, y, block, 2, 2);
            }
        }
    }

    private void drawFood(Graphics2D g) {
        int tile = getTileSize();
        int boardX = getBoardX();
        int boardY = getBoardY();

        g.setColor(LCD_DARK);
        drawFoodSprite(g, boardX + food.x * tile, boardY + food.y * tile, tile);
    }

    /**
     * Futter-Sprite im ursprünglichen Stil (wie zu Beginn).
     */
    private void drawFoodSprite(Graphics2D g, int x, int y, int size) {
        int block = size / 6;

        int[][] sprite = {
                {2, 0},
                {1, 1}, {2, 1}, {3, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2},
                {1, 3}, {2, 3}
        };

        for (int[] p : sprite) {
            g.fillRect(x + p[0] * block + block, y + p[1] * block + block, block, block);
        }
    }

    private void fillPixel(Graphics2D g, int x, int y, int block, int px, int py) {
        g.fillRect(x + px * block, y + py * block, block, block);
    }

    private void drawCenterMessage(Graphics2D g, String title, String subtitle) {
        int boardX = getBoardX();
        int boardY = getBoardY();
        int boardW = getBoardWidth();
        int boardH = getBoardHeight();

        int boxW = Math.min(280, boardW - 30);
        int boxH = 90;
        int boxX = boardX + (boardW - boxW) / 2;
        int boxY = boardY + (boardH - boxH) / 2;

        g.setColor(LCD_BG);
        g.fillRect(boxX, boxY, boxW, boxH);

        g.setColor(LCD_DARK);
        g.drawRect(boxX, boxY, boxW, boxH);

        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(title, boxX + (boxW - fm1.stringWidth(title)) / 2, boxY + 32);

        g.setFont(new Font("Monospaced", Font.PLAIN, 16));
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(subtitle, boxX + (boxW - fm2.stringWidth(subtitle)) / 2, boxY + 60);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                if (direction != Direction.DOWN) nextDirection = Direction.UP;
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (direction != Direction.UP) nextDirection = Direction.DOWN;
            }
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                if (direction != Direction.RIGHT) nextDirection = Direction.LEFT;
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (direction != Direction.LEFT) nextDirection = Direction.RIGHT;
            }
            case KeyEvent.VK_SPACE -> {
                if (!gameOver) paused = !paused;
            }
            case KeyEvent.VK_R -> startGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Classic");
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