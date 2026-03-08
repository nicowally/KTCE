package SnakeOld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    // ===== Window / Grid logic (nur intern, nicht sichtbar) =====
    private static final int TILE_SIZE = 18;
    private static final int GRID_WIDTH = 36;
    private static final int GRID_HEIGHT = 26;
    private static final int WINDOW_WIDTH = GRID_WIDTH * TILE_SIZE;
    private static final int WINDOW_HEIGHT = GRID_HEIGHT * TILE_SIZE;

    // ===== Timing =====
    private static final int FRAME_DELAY_MS = 16;   // ~60 FPS
    private static final int MOVE_DELAY_MS = 85;

    private final Timer frameTimer;
    private long lastMoveTime;

    // ===== Spielstatus =====
    private final List<SnakeSegment> snake = new ArrayList<>();
    private final Random random = new Random();

    private Point foodGrid;
    private FruitType currentFruit;

    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;

    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private int score = 0;

    public SnakeGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        startGame();

        frameTimer = new Timer(FRAME_DELAY_MS, this);
        frameTimer.start();
    }

    // ===== Helper Types =====

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        boolean isOpposite(Direction other) {
            return this.dx == -other.dx && this.dy == -other.dy;
        }

        double angle() {
            return Math.atan2(dy, dx);
        }
    }

    private enum FruitType {
        APPLE, PEAR, ORANGE, GRAPES, CHERRY
    }

    private static class SnakeSegment {
        int gridX;
        int gridY;
        int prevGridX;
        int prevGridY;

        SnakeSegment(int x, int y) {
            this.gridX = x;
            this.gridY = y;
            this.prevGridX = x;
            this.prevGridY = y;
        }

        void setPosition(int x, int y) {
            this.prevGridX = this.gridX;
            this.prevGridY = this.gridY;
            this.gridX = x;
            this.gridY = y;
        }
    }

    private static class RenderPoint {
        final float x;
        final float y;

        RenderPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    // ===== Setup =====

    private void startGame() {
        snake.clear();

        snake.add(new SnakeSegment(8, 8));
        snake.add(new SnakeSegment(7, 8));
        snake.add(new SnakeSegment(6, 8));
        snake.add(new SnakeSegment(5, 8));
        snake.add(new SnakeSegment(4, 8));

        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;

        running = true;
        gameOver = false;
        paused = false;
        score = 0;

        spawnFood();
        lastMoveTime = System.currentTimeMillis();
    }

    private void spawnFood() {
        Point candidate;
        do {
            candidate = new Point(random.nextInt(GRID_WIDTH), random.nextInt(GRID_HEIGHT));
        } while (isOccupiedBySnake(candidate.x, candidate.y));

        foodGrid = candidate;
        FruitType[] fruits = FruitType.values();
        currentFruit = fruits[random.nextInt(fruits.length)];
    }

    private boolean isOccupiedBySnake(int x, int y) {
        for (SnakeSegment segment : snake) {
            if (segment.gridX == x && segment.gridY == y) {
                return true;
            }
        }
        return false;
    }

    // ===== Loop =====

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            long now = System.currentTimeMillis();
            if (now - lastMoveTime >= MOVE_DELAY_MS) {
                updateGame();
                lastMoveTime = now;
            }
        }
        repaint();
    }

    private void updateGame() {
        if (nextDirection != null && !nextDirection.isOpposite(currentDirection)) {
            currentDirection = nextDirection;
        }

        SnakeSegment head = snake.get(0);
        int newHeadX = head.gridX + currentDirection.dx;
        int newHeadY = head.gridY + currentDirection.dy;

        if (newHeadX < 0 || newHeadX >= GRID_WIDTH || newHeadY < 0 || newHeadY >= GRID_HEIGHT) {
            running = false;
            gameOver = true;
            return;
        }

        boolean willGrow = (newHeadX == foodGrid.x && newHeadY == foodGrid.y);

        for (int i = 0; i < snake.size(); i++) {
            if (!willGrow && i == snake.size() - 1) {
                continue;
            }
            SnakeSegment segment = snake.get(i);
            if (segment.gridX == newHeadX && segment.gridY == newHeadY) {
                running = false;
                gameOver = true;
                return;
            }
        }

        int[] oldX = new int[snake.size()];
        int[] oldY = new int[snake.size()];
        for (int i = 0; i < snake.size(); i++) {
            oldX[i] = snake.get(i).gridX;
            oldY[i] = snake.get(i).gridY;
        }

        snake.get(0).setPosition(newHeadX, newHeadY);

        for (int i = 1; i < snake.size(); i++) {
            snake.get(i).setPosition(oldX[i - 1], oldY[i - 1]);
        }

        if (willGrow) {
            score++;
            SnakeSegment newTail = new SnakeSegment(oldX[oldX.length - 1], oldY[oldY.length - 1]);
            newTail.prevGridX = oldX[oldX.length - 1];
            newTail.prevGridY = oldY[oldY.length - 1];
            snake.add(newTail);
            spawnFood();
        }
    }

    // ===== Render =====

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float progress = getMoveProgress();
        double time = System.currentTimeMillis() / 1000.0;

        drawBackground(g2, time);
        drawFood(g2, time);
        drawSnakeOrganic(g2, progress, time);
        drawHud(g2);

        if (paused && running) {
            drawCenteredOverlay(g2, "PAUSED", "Press SPACE to continue");
        }

        if (gameOver) {
            drawCenteredOverlay(g2, "GAME OVER", "Press R to restart");
        }

        g2.dispose();
    }

    private float getMoveProgress() {
        long elapsed = System.currentTimeMillis() - lastMoveTime;
        float progress = elapsed / (float) MOVE_DELAY_MS;
        return Math.max(0f, Math.min(progress, 1f));
    }

    private void drawBackground(Graphics2D g2, double time) {
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(155, 223, 124),
                0, WINDOW_HEIGHT, new Color(86, 156, 74)
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // weiche Lichtflächen
        g2.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 12; i++) {
            int size = 90 + pseudoRandom(i * 11) % 180;
            int x = (pseudoRandom(i * 17) % (WINDOW_WIDTH + 100)) - 50;
            int y = (pseudoRandom(i * 23) % (WINDOW_HEIGHT + 100)) - 50;
            g2.fillOval(x, y, size, size);
        }

        // kleine Grashalme / organische Deko
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 160; i++) {
            int x = pseudoRandom(i * 31) % WINDOW_WIDTH;
            int y = pseudoRandom(i * 47) % WINDOW_HEIGHT;
            int h = 4 + pseudoRandom(i * 7) % 8;

            float sway = (float) Math.sin(time * 1.6 + i * 0.7) * 1.8f;

            g2.setColor(new Color(60, 125, 55, 55));
            g2.draw(new QuadCurve2D.Float(
                    x, y,
                    x + sway, y - h / 2f,
                    x + sway * 1.4f, y - h
            ));
        }
    }

    private int pseudoRandom(int seed) {
        int value = seed * 1103515245 + 12345;
        return Math.abs(value);
    }

    // ===== Snake =====

    private void drawSnakeOrganic(Graphics2D g2, float progress, double time) {
        List<RenderPoint> points = getInterpolatedSnakeCenters(progress);

        List<RenderPoint> slithered = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            RenderPoint p = points.get(i);

            double nx = 0;
            double ny = 0;

            if (i < points.size() - 1) {
                RenderPoint next = points.get(i + 1);
                double dx = next.x - p.x;
                double dy = next.y - p.y;
                double len = Math.hypot(dx, dy);
                if (len > 0.0001) {
                    nx = -dy / len;
                    ny = dx / len;
                }
            } else if (i > 0) {
                RenderPoint prev = points.get(i - 1);
                double dx = p.x - prev.x;
                double dy = p.y - prev.y;
                double len = Math.hypot(dx, dy);
                if (len > 0.0001) {
                    nx = -dy / len;
                    ny = dx / len;
                }
            }

            double amplitude = Math.max(0.0, 3.8 - i * 0.33);
            double wave = Math.sin(time * 8.5 - i * 0.8) * amplitude;

            float sx = (float) (p.x + nx * wave);
            float sy = (float) (p.y + ny * wave);

            if (i == 0) {
                sx = p.x;
                sy = p.y;
            }

            slithered.add(new RenderPoint(sx, sy));
        }

        drawSnakeShadow(g2, slithered);
        drawSnakeBody(g2, slithered);
        drawSnakePattern(g2, slithered);
        drawSnakeHead(g2, slithered, time);
    }

    private List<RenderPoint> getInterpolatedSnakeCenters(float progress) {
        List<RenderPoint> points = new ArrayList<>();
        for (SnakeSegment segment : snake) {
            float gx = lerp(segment.prevGridX, segment.gridX, progress);
            float gy = lerp(segment.prevGridY, segment.gridY, progress);

            float px = gx * TILE_SIZE + TILE_SIZE / 2f;
            float py = gy * TILE_SIZE + TILE_SIZE / 2f;

            points.add(new RenderPoint(px, py));
        }
        return points;
    }

    private float lerp(int a, int b, float t) {
        return a + (b - a) * t;
    }

    private void drawSnakeShadow(Graphics2D g2, List<RenderPoint> points) {
        Path2D.Float path = buildSmoothPath(points);

        AffineTransform old = g2.getTransform();
        g2.translate(3, 4);
        g2.setColor(new Color(0, 0, 0, 50));
        g2.setStroke(new BasicStroke(17f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
        g2.setTransform(old);
    }

    private void drawSnakeBody(Graphics2D g2, List<RenderPoint> points) {
        Path2D.Float path = buildSmoothPath(points);

        g2.setColor(new Color(34, 118, 52));
        g2.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        g2.setColor(new Color(82, 204, 104));
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawSnakePattern(Graphics2D g2, List<RenderPoint> points) {
        for (int i = 1; i < points.size(); i++) {
            RenderPoint p = points.get(i);
            float r = Math.max(2.8f, 6f - i * 0.18f);

            g2.setColor(new Color(255, 255, 255, 25));
            g2.fill(new Ellipse2D.Float(p.x - r * 0.8f, p.y - r * 0.95f, r * 1.5f, r * 0.75f));

            if (i % 2 == 0) {
                g2.setColor(new Color(23, 80, 35, 65));
                g2.fill(new Ellipse2D.Float(p.x - r * 0.65f, p.y - r * 0.1f, r * 1.25f, r * 0.85f));
            }
        }
    }

    private Path2D.Float buildSmoothPath(List<RenderPoint> points) {
        Path2D.Float path = new Path2D.Float();
        if (points.isEmpty()) {
            return path;
        }

        if (points.size() == 1) {
            RenderPoint p = points.get(0);
            path.moveTo(p.x, p.y);
            path.lineTo(p.x, p.y);
            return path;
        }

        path.moveTo(points.get(0).x, points.get(0).y);

        for (int i = 1; i < points.size() - 1; i++) {
            RenderPoint current = points.get(i);
            RenderPoint next = points.get(i + 1);

            float midX = (current.x + next.x) / 2f;
            float midY = (current.y + next.y) / 2f;

            path.quadTo(current.x, current.y, midX, midY);
        }

        RenderPoint last = points.get(points.size() - 1);
        path.lineTo(last.x, last.y);

        return path;
    }

    private void drawSnakeHead(Graphics2D g2, List<RenderPoint> points, double time) {
        if (points.isEmpty()) return;

        RenderPoint head = points.get(0);
        double angle = currentDirection.angle();

        float headW = 22f;
        float headH = 17f;

        AffineTransform old = g2.getTransform();
        g2.translate(head.x, head.y);
        g2.rotate(angle);

        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new Ellipse2D.Float(-headW / 2f + 2, -headH / 2f + 3, headW, headH));

        g2.setColor(new Color(32, 116, 50));
        g2.fill(new Ellipse2D.Float(-headW / 2f, -headH / 2f, headW, headH));

        g2.setColor(new Color(88, 210, 108));
        g2.fill(new Ellipse2D.Float(-headW / 2f + 2, -headH / 2f + 2, headW - 4, headH - 4));

        g2.setColor(new Color(255, 255, 255, 45));
        g2.fill(new Ellipse2D.Float(-3, -6, 8, 4));

        g2.setColor(new Color(95, 225, 118, 170));
        g2.fill(new Ellipse2D.Float(3, -4, 7, 8));

        drawEyes(g2, time);
        drawTongue(g2, time);

        g2.setTransform(old);
    }

    private void drawEyes(Graphics2D g2, double time) {
        boolean blink = isBlinking(time);

        float leftEyeX = 2.2f;
        float leftEyeY = -4.8f;
        float rightEyeX = 2.2f;
        float rightEyeY = 4.8f;

        if (blink) {
            g2.setColor(new Color(20, 60, 20));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(leftEyeX - 2, leftEyeY, leftEyeX + 2, leftEyeY));
            g2.draw(new Line2D.Float(rightEyeX - 2, rightEyeY, rightEyeX + 2, rightEyeY));
            return;
        }

        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(leftEyeX - 2.2f, leftEyeY - 2.2f, 4.4f, 4.4f));
        g2.fill(new Ellipse2D.Float(rightEyeX - 2.2f, rightEyeY - 2.2f, 4.4f, 4.4f));

        g2.setColor(Color.BLACK);
        g2.fill(new Ellipse2D.Float(leftEyeX - 0.9f, leftEyeY - 0.9f, 1.8f, 1.8f));
        g2.fill(new Ellipse2D.Float(rightEyeX - 0.9f, rightEyeY - 0.9f, 1.8f, 1.8f));
    }

    private boolean isBlinking(double time) {
        double cycle = time % 3.4;
        return (cycle > 0.00 && cycle < 0.08) || (cycle > 0.15 && cycle < 0.22);
    }

    private void drawTongue(Graphics2D g2, double time) {
        if (!isTongueOut(time) || !running || gameOver || paused) {
            return;
        }

        double phase = (time % 2.8) / 2.8; // alle paar Sekunden
        float baseX = 10f;
        float baseY = 0f;
        float length = 8f + (float) Math.sin(phase * Math.PI) * 5f;

        Path2D.Float tongue = new Path2D.Float();
        tongue.moveTo(baseX, baseY);
        tongue.lineTo(baseX + length, -1.3f);
        tongue.lineTo(baseX + length + 4f, -4f);

        tongue.moveTo(baseX + length, -1.3f);
        tongue.lineTo(baseX + length + 4f, 1.4f);

        g2.setColor(new Color(221, 65, 96));
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(tongue);
    }

    private boolean isTongueOut(double time) {
        double cycle = time % 2.8;
        return cycle > 0.10 && cycle < 0.34;
    }

    // ===== Food =====

    private void drawFood(Graphics2D g2, double time) {
        int x = foodGrid.x * TILE_SIZE;
        int y = foodGrid.y * TILE_SIZE;

        float bob = (float) Math.sin(time * 3.0 + foodGrid.x * 0.7 + foodGrid.y * 0.4) * 1.5f;

        Graphics2D gf = (Graphics2D) g2.create();
        gf.translate(0, bob);

        // kleiner Schatten
        gf.setColor(new Color(0, 0, 0, 35));
        gf.fillOval(x + 4, y + 13, 12, 4);

        switch (currentFruit) {
            case APPLE -> drawApple(gf, x, y);
            case PEAR -> drawPear(gf, x, y);
            case ORANGE -> drawOrange(gf, x, y);
            case GRAPES -> drawGrapes(gf, x, y);
            case CHERRY -> drawCherry(gf, x, y);
        }

        gf.dispose();
    }

    private void drawApple(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(216, 50, 50));
        g2.fillOval(x + 4, y + 5, 10, 10);
        g2.fillOval(x + 8, y + 5, 10, 10);
        g2.setColor(new Color(100, 60, 30));
        g2.fillRect(x + 8, y + 2, 2, 5);
        g2.setColor(new Color(70, 180, 75));
        g2.fillOval(x + 10, y + 2, 6, 4);
    }

    private void drawPear(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(184, 214, 77));
        g2.fillOval(x + 6, y + 3, 7, 7);
        g2.fillOval(x + 4, y + 8, 11, 9);
        g2.setColor(new Color(100, 60, 30));
        g2.fillRect(x + 8, y + 1, 2, 4);
        g2.setColor(new Color(70, 180, 75));
        g2.fillOval(x + 10, y + 1, 5, 3);
    }

    private void drawOrange(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 151, 41));
        g2.fillOval(x + 4, y + 4, 11, 11);
        g2.setColor(new Color(255, 210, 120, 90));
        g2.fillOval(x + 7, y + 6, 4, 4);
        g2.setColor(new Color(70, 180, 75));
        g2.fillOval(x + 9, y + 2, 5, 3);
    }

    private void drawGrapes(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(120, 60, 180));
        g2.fillOval(x + 7, y + 4, 5, 5);
        g2.fillOval(x + 4, y + 8, 5, 5);
        g2.fillOval(x + 10, y + 8, 5, 5);
        g2.fillOval(x + 7, y + 11, 5, 5);
        g2.setColor(new Color(70, 180, 75));
        g2.fillOval(x + 9, y + 2, 5, 3);
    }

    private void drawCherry(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(180, 20, 20));
        g2.fillOval(x + 4, y + 9, 6, 6);
        g2.fillOval(x + 10, y + 9, 6, 6);
        g2.setColor(new Color(70, 180, 75));
        g2.drawLine(x + 7, y + 9, x + 10, y + 4);
        g2.drawLine(x + 13, y + 9, x + 10, y + 4);
    }

    // ===== HUD =====

    private void drawHud(Graphics2D g2) {
        g2.setColor(new Color(20, 28, 20, 145));
        g2.fillRoundRect(12, 12, 145, 38, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Score: " + score, 28, 37);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString("SPACE pause", WINDOW_WIDTH - 110, 28);
        g2.drawString("R restart", WINDOW_WIDTH - 90, 46);
    }

    private void drawCenteredOverlay(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int boxW = 280;
        int boxH = 120;
        int boxX = (WINDOW_WIDTH - boxW) / 2;
        int boxY = (WINDOW_HEIGHT - boxH) / 2;

        g2.setColor(new Color(28, 36, 28, 220));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2.setColor(new Color(255, 255, 255, 35));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fmTitle = g2.getFontMetrics();
        g2.drawString(title, (WINDOW_WIDTH - fmTitle.stringWidth(title)) / 2, boxY + 45);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        FontMetrics fmSub = g2.getFontMetrics();
        g2.drawString(subtitle, (WINDOW_WIDTH - fmSub.stringWidth(subtitle)) / 2, boxY + 80);
    }

    // ===== Input =====

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                if (currentDirection != Direction.DOWN) {
                    nextDirection = Direction.UP;
                }
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (currentDirection != Direction.UP) {
                    nextDirection = Direction.DOWN;
                }
            }
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                if (currentDirection != Direction.RIGHT) {
                    nextDirection = Direction.LEFT;
                }
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (currentDirection != Direction.LEFT) {
                    nextDirection = Direction.RIGHT;
                }
            }
            case KeyEvent.VK_SPACE -> {
                if (running && !gameOver) {
                    paused = !paused;
                }
            }
            case KeyEvent.VK_R -> startGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ===== Main =====

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Modern Snake");
            SnakeGame game = new SnakeGame();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}