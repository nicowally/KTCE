package SnakeNew.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SnakeClient extends JPanel implements KeyListener {
    private static final int TILE_SIZE = 20;
    private static final int GRID_WIDTH = 36;
    private static final int GRID_HEIGHT = 26;
    private static final int WINDOW_WIDTH = GRID_WIDTH * TILE_SIZE;
    private static final int WINDOW_HEIGHT = GRID_HEIGHT * TILE_SIZE;

    private final String host;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int myPlayerId = 0;
    private String status = "CONNECTING";
    private int score1 = 0;
    private int score2 = 0;
    private Point food = new Point(0, 0);
    private List<Point> snake1 = new ArrayList<>();
    private List<Point> snake2 = new ArrayList<>();

    public SnakeClient(String host) {
        this.host = host;
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        connect();

        Timer repaintTimer = new Timer(16, e -> repaint());
        repaintTimer.start();
    }

    private void connect() {
        try {
            socket = new Socket(host, 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            new Thread(this::listen, "snake-client-listener").start();
        } catch (IOException e) {
            status = "DISCONNECTED";
            e.printStackTrace();
        }
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            status = "DISCONNECTED";
        }
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("WELCOME|")) {
            myPlayerId = Integer.parseInt(msg.split("\\|")[1]);
        } else if (msg.startsWith("STATE|")) {
            String[] parts = msg.split("\\|", -1);
            status = parts[1];
            score1 = Integer.parseInt(parts[2]);
            score2 = Integer.parseInt(parts[3]);
            food = new Point(Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
            snake1 = parseSnake(parts[6]);
            snake2 = parseSnake(parts[7]);
        }
    }

    private List<Point> parseSnake(String encoded) {
        List<Point> result = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return result;

        String[] segments = encoded.split(";");
        for (String s : segments) {
            String[] xy = s.split(",");
            result.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);
        drawFood(g2);
        drawSnake(g2, snake1, new Color(70, 210, 110), new Color(40, 130, 70));
        drawSnake(g2, snake2, new Color(80, 155, 255), new Color(45, 90, 190));
        drawHud(g2);
        drawOverlay(g2);

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(153, 224, 124),
                0, WINDOW_HEIGHT, new Color(82, 156, 70)
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2.setColor(new Color(255, 255, 255, 18));
        for (int i = 0; i < 12; i++) {
            int size = 90 + (i * 37 % 100);
            int x = (i * 97) % WINDOW_WIDTH - 40;
            int y = (i * 131) % WINDOW_HEIGHT - 40;
            g2.fillOval(x, y, size, size);
        }
    }

    private void drawSnake(Graphics2D g2, List<Point> snake, Color inner, Color outer) {
        if (snake.isEmpty()) return;

        Path2D.Float path = buildSmoothPath(snake);

        g2.setColor(new Color(0, 0, 0, 50));
        AffineTransform old = g2.getTransform();
        g2.translate(3, 4);
        g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
        g2.setTransform(old);

        g2.setColor(outer);
        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        g2.setColor(inner);
        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        Point head = snake.get(0);
        float cx = head.x * TILE_SIZE + TILE_SIZE / 2f;
        float cy = head.y * TILE_SIZE + TILE_SIZE / 2f;

        g2.setColor(outer);
        g2.fill(new Ellipse2D.Float(cx - 11, cy - 9, 22, 18));

        g2.setColor(inner);
        g2.fill(new Ellipse2D.Float(cx - 9, cy - 7, 18, 14));

        g2.setColor(Color.WHITE);
        g2.fillOval((int) cx + 2, (int) cy - 5, 4, 4);
        g2.fillOval((int) cx + 2, (int) cy + 1, 4, 4);

        g2.setColor(Color.BLACK);
        g2.fillOval((int) cx + 3, (int) cy - 4, 2, 2);
        g2.fillOval((int) cx + 3, (int) cy + 2, 2, 2);
    }

    private Path2D.Float buildSmoothPath(List<Point> snake) {
        List<Point2D.Float> pts = new ArrayList<>();
        for (Point p : snake) {
            pts.add(new Point2D.Float(
                    p.x * TILE_SIZE + TILE_SIZE / 2f,
                    p.y * TILE_SIZE + TILE_SIZE / 2f
            ));
        }

        Path2D.Float path = new Path2D.Float();
        if (pts.isEmpty()) return path;

        path.moveTo(pts.get(0).x, pts.get(0).y);

        if (pts.size() == 1) {
            path.lineTo(pts.get(0).x, pts.get(0).y);
            return path;
        }

        for (int i = 1; i < pts.size() - 1; i++) {
            Point2D.Float current = pts.get(i);
            Point2D.Float next = pts.get(i + 1);
            float midX = (current.x + next.x) / 2f;
            float midY = (current.y + next.y) / 2f;
            path.quadTo(current.x, current.y, midX, midY);
        }

        Point2D.Float last = pts.get(pts.size() - 1);
        path.lineTo(last.x, last.y);

        return path;
    }

    private void drawFood(Graphics2D g2) {
        int x = food.x * TILE_SIZE;
        int y = food.y * TILE_SIZE;

        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillOval(x + 4, y + 14, 12, 4);

        g2.setColor(new Color(216, 50, 50));
        g2.fillOval(x + 4, y + 5, 10, 10);
        g2.fillOval(x + 8, y + 5, 10, 10);

        g2.setColor(new Color(100, 60, 30));
        g2.fillRect(x + 8, y + 2, 2, 5);

        g2.setColor(new Color(70, 180, 75));
        g2.fillOval(x + 10, y + 2, 6, 4);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(new Color(20, 28, 20, 150));
        g2.fillRoundRect(12, 12, 230, 42, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));

        String me = myPlayerId == 1 ? "Du = Grün" : "Du = Blau";
        g2.drawString("P1: " + score1 + "   P2: " + score2 + "   " + me, 24, 39);
    }

    private void drawOverlay(Graphics2D g2) {
        if ("RUNNING".equals(status)) return;

        String title;
        if ("WAITING".equals(status)) {
            title = "Waiting for second player...";
        } else if ("P1_WIN".equals(status)) {
            title = myPlayerId == 1 ? "You win!" : "You lose!";
        } else if ("P2_WIN".equals(status)) {
            title = myPlayerId == 2 ? "You win!" : "You lose!";
        } else if ("DRAW".equals(status)) {
            title = "Draw!";
        } else if ("CONNECTING".equals(status)) {
            title = "Connecting...";
        } else {
            title = "Disconnected";
        }

        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int boxW = 360;
        int boxH = 130;
        int boxX = (WINDOW_WIDTH - boxW) / 2;
        int boxY = (WINDOW_HEIGHT - boxH) / 2;

        g2.setColor(new Color(28, 36, 28, 220));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, boxY + 48);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String sub = "Press R to request a new round";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(sub, (WINDOW_WIDTH - fm2.stringWidth(sub)) / 2, boxY + 85);
    }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> send("UP");
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> send("DOWN");
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> send("LEFT");
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> send("RIGHT");
            case KeyEvent.VK_R -> send("RESTART");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private record Point(int x, int y) {}

    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog(null, "Server-IP eingeben:", "127.0.0.1");
        if (host == null || host.isBlank()) return;

        String finalHost = host.trim();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake LAN Client");
            SnakeClient client = new SnakeClient(finalHost);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(client);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            client.requestFocusInWindow();
        });
    }
}