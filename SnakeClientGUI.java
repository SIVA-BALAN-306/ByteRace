import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The client GUI, updated to automatically close on game over.
 */
public class SnakeClientGUI extends JFrame {
    private GamePanel gamePanel;
    private LeaderboardPanel leaderboardPanel;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Socket socket;
    private int myId = -1;

    public SnakeClientGUI() {
        setTitle("Snake Royale");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(new Color(25, 25, 30));
        setContentPane(contentPane);

        gamePanel = new GamePanel();
        leaderboardPanel = new LeaderboardPanel();
        
        contentPane.add(gamePanel, BorderLayout.CENTER);
        contentPane.add(leaderboardPanel, BorderLayout.EAST);

        // pack() creates a window of the preferred size, it is not maximized by default.
        pack();
        setLocationRelativeTo(null);
        
        setupKeyListener();
        
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                try { if (socket != null) socket.close(); } catch (IOException ex) {}
                System.exit(0);
            }
        });
    }

    private void setupKeyListener() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    sendMessage(new Message(Message.MessageType.BOOST, true));
                } else {
                    handleMoveKeys(e.getKeyCode());
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    sendMessage(new Message(Message.MessageType.BOOST, false));
                }
            }
        });
        setFocusable(true);
    }
    
    private void handleMoveKeys(int keyCode) {
        Snake.Direction dir = null;
        switch (keyCode) {
            case KeyEvent.VK_UP: case KeyEvent.VK_W: dir = Snake.Direction.UP; break;
            case KeyEvent.VK_DOWN: case KeyEvent.VK_S: dir = Snake.Direction.DOWN; break;
            case KeyEvent.VK_LEFT: case KeyEvent.VK_A: dir = Snake.Direction.LEFT; break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: dir = Snake.Direction.RIGHT; break;
        }
        if (dir != null) {
            sendMessage(new Message(Message.MessageType.MOVE, dir));
        }
    }

    private void connectAndListen() {
        // A more visually appealing connection dialog
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
        labels.add(new JLabel("Server IP:", SwingConstants.RIGHT));
        labels.add(new JLabel("Your Name:", SwingConstants.RIGHT));
        labels.add(new JLabel("Symbol (1 Char):", SwingConstants.RIGHT));
        panel.add(labels, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField serverAddressField = new JTextField("localhost", 15);
        JTextField playerNameField = new JTextField("Player" + (int)(Math.random() * 100), 15);
        JTextField playerSymbolField = new JTextField("S", 15);
        controls.add(serverAddressField);
        controls.add(playerNameField);
        controls.add(playerSymbolField);
        panel.add(controls, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(this, panel, "Connect to Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) { System.exit(0); }
        
        String serverAddress = serverAddressField.getText();
        String playerName = playerNameField.getText();
        String playerSymbol = playerSymbolField.getText().trim();
        if (playerSymbol.length() > 1) playerSymbol = playerSymbol.substring(0, 1);
        if (playerSymbol.isEmpty()) playerSymbol = "?";
        
        try {
            socket = new Socket(serverAddress, 12345);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            sendMessage(new Message(Message.MessageType.JOIN, new String[]{playerName, playerSymbol}));
            
            setVisible(true);
            new Thread(this::listenToServer).start();

        } catch (UnknownHostException e) {
            showErrorDialog("Server not found: " + e.getMessage());
        } catch (IOException e) {
            showErrorDialog("Could not connect: " + e.getMessage());
        }
    }
    
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private void listenToServer() {
        try {
            while (true) {
                Message msg = (Message) ois.readObject();
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (Exception e) {
            if (!socket.isClosed()) {
                SwingUtilities.invokeLater(() -> showErrorDialog("Lost connection to the server."));
            }
        }
    }
    
    private void handleServerMessage(Message msg) {
        switch (msg.getType()) {
            case GAME_STATE:
                Message.GameState gs = (Message.GameState) msg.getPayload();
                gamePanel.updateState(gs, myId);
                leaderboardPanel.updateState(gs, myId);
                break;
            case PLAYER_INFO:
                setTitle("Snake Royale - You are the " + colorToString((Color)msg.getPayload()) + " snake!");
                break;
            case YOUR_ID:
                this.myId = (int) msg.getPayload();
                break;
            case COUNTDOWN:
                gamePanel.setCountdown((int) msg.getPayload());
                break;
            case GAME_OVER:
                 // Show the final message, then dispose of the window and exit the application.
                 JOptionPane.showMessageDialog(this, msg.getPayload().toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
                 dispose();
                 System.exit(0);
                break;
            case ERROR:
                 showErrorDialog("Server error: " + msg.getPayload().toString());
                 break;
            default: break;
        }
    }

    private void sendMessage(Message msg) {
        try {
            if (oos != null) {
                oos.writeObject(msg);
                oos.flush();
            }
        } catch (IOException e) { /* ignore */ }
    }

    private String colorToString(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float hue = hsb[0] * 360;
        if (hue < 30) return "Reddish";
        if (hue < 90) return "Yellowish";
        if (hue < 150) return "Greenish";
        if (hue < 210) return "Cyan";
        if (hue < 270) return "Blueish";
        if (hue < 330) return "Magenta";
        return "Reddish";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SnakeClientGUI().connectAndListen());
    }
}

/**
 * Custom JPanel to draw the leaderboard.
 */
class LeaderboardPanel extends JPanel {
    private Message.GameState gameState;
    private int myId = -1;
    private Font boldFont = new Font("Segoe UI", Font.BOLD, 14);
    private Font normalFont = new Font("Segoe UI", Font.PLAIN, 12);
    
    LeaderboardPanel() {
        setPreferredSize(new Dimension(250, 800));
        setBackground(new Color(40, 42, 54));
    }
    
    public void updateState(Message.GameState gs, int myId) {
        this.gameState = gs;
        this.myId = myId;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.setFont(boldFont.deriveFont(18f));
        g2d.drawString("Leaderboard", 20, 30);
        
        if (gameState == null) return;
        
        List<Snake> sortedSnakes = gameState.snakes.values().stream()
            .sorted(Comparator.comparingInt((Snake s) -> s.score).reversed())
            .collect(Collectors.toList());

        int y = 70;
        for (int i = 0; i < sortedSnakes.size(); i++) {
            Snake snake = sortedSnakes.get(i);
            
            int snakeId = gameState.snakes.entrySet().stream()
                .filter(entry -> entry.getValue() == snake)
                .map(Map.Entry::getKey)
                .findFirst().orElse(-1);

            String name = gameState.playerNames.getOrDefault(snakeId, "Unknown");
            
            if (snakeId == myId) {
                g2d.setColor(new Color(80, 85, 100));
                g2d.fillRect(10, y - 20, getWidth() - 20, 65);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(boldFont);
            g2d.drawString((i+1) + ". " + name, 20, y);

            g2d.setFont(normalFont);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Score: " + snake.score, 20, y + 20);

            g2d.drawString("Boost:", 20, y + 40);
            g2d.setColor(new Color(60, 60, 70));
            g2d.fillRect(65, y + 30, 150, 10);
            g2d.setColor(snake.isBoosting ? Color.ORANGE : Color.CYAN);
            g2d.fillRect(65, y + 30, (int)(150 * (snake.powerUpMeter / 100.0)), 10);
            
            y += 75;
        }
    }
}

/**
 * Custom JPanel to draw the game world with a dynamic size.
 */
class GamePanel extends JPanel {
    private Message.GameState gameState;
    private int myId = -1;
    private int countdown = -1;
    private final int CELL_SIZE = 25;
    
    private int boardWidth = 80;
    private int boardHeight = 80;

    GamePanel() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(new Color(20, 22, 30));
    }

    public void updateState(Message.GameState newState, int myId) {
        this.gameState = newState;
        this.myId = myId;
        this.boardWidth = newState.boardWidth;
        this.boardHeight = newState.boardHeight;
        repaint();
    }
    
    public void setCountdown(int count) {
        this.countdown = count;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (myId == -1 || gameState == null || !gameState.snakes.containsKey(myId)) {
            drawWaitingScreen(g2d);
            return;
        }
        
        Snake mySnake = gameState.snakes.get(myId);
        Point myHead = mySnake.getHead();
        
        int cameraX = myHead.x * CELL_SIZE - getWidth() / 2;
        int cameraY = myHead.y * CELL_SIZE - getHeight() / 2;
        
        cameraX = Math.max(0, Math.min(boardWidth * CELL_SIZE - getWidth(), cameraX));
        cameraY = Math.max(0, Math.min(boardHeight * CELL_SIZE - getHeight(), cameraY));

        g2d.translate(-cameraX, -cameraY);
        
        drawBoundary(g2d);
        drawGrid(g2d);
        drawFood(g2d);
        drawSnakes(g2d);
        
        g2d.translate(cameraX, cameraY);
        if (countdown > 0) {
            drawCountdown(g2d);
        } else if (countdown == 0) {
            drawGoText(g2d);
            countdown = -1;
        }
    }

    private void drawBoundary(Graphics2D g2d) {
        g2d.setColor(new Color(100, 120, 200, 200));
        g2d.setStroke(new BasicStroke(10));
        g2d.drawRect(0, 0, boardWidth * CELL_SIZE, boardHeight * CELL_SIZE);
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(50, 52, 70, 100));
        for (int i = 0; i <= boardWidth; i++) {
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, boardHeight * CELL_SIZE);
        }
        for (int i = 0; i <= boardHeight; i++) {
            g2d.drawLine(0, i * CELL_SIZE, boardWidth * CELL_SIZE, i * CELL_SIZE);
        }
    }
    
    private void drawFood(Graphics2D g2d) {
        if (gameState == null || gameState.foodItems == null) return;
        for(Message.Food food : gameState.foodItems) {
            int x = food.location.x * CELL_SIZE;
            int y = food.location.y * CELL_SIZE;
            
            g2d.setColor(food.color);
            g2d.fill(new Ellipse2D.Double(x, y, CELL_SIZE, CELL_SIZE));
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x + CELL_SIZE/2 - 2, y - 5, 4, 7);
            g2d.setColor(Color.GREEN.darker());
            g2d.fillOval(x + CELL_SIZE/2, y - 8, 10, 5);
        }
    }

    private void drawSnakes(Graphics2D g2d) {
        if (gameState == null || gameState.snakes == null) return;
        for (Snake snake : gameState.snakes.values()) {
            Color snakeColor = snake.getColor();
            List<Point> body = snake.getBody();

            g2d.setColor(snakeColor.darker());
            for (int i = 1; i < body.size(); i++) {
                Point p = body.get(i);
                g2d.fill(new RoundRectangle2D.Double(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE, 15, 15));
            }

            Point head = snake.getHead();
            int hx = head.x * CELL_SIZE;
            int hy = head.y * CELL_SIZE;
            g2d.setColor(snakeColor);
            g2d.fill(new Ellipse2D.Double(hx, hy, CELL_SIZE, CELL_SIZE));

            g2d.setColor(Color.WHITE);
            int eyeSize = CELL_SIZE / 4;
            int pupilSize = eyeSize / 2;
            Point eye1 = new Point(), eye2 = new Point();
            switch (snake.getDirection()) {
                case UP:
                    eye1.setLocation(hx + eyeSize/2, hy + eyeSize/2);
                    eye2.setLocation(hx + CELL_SIZE - eyeSize - eyeSize/2, hy + eyeSize/2);
                    break;
                case DOWN:
                    eye1.setLocation(hx + eyeSize/2, hy + CELL_SIZE - eyeSize - eyeSize/2);
                    eye2.setLocation(hx + CELL_SIZE - eyeSize - eyeSize/2, hy + CELL_SIZE - eyeSize - eyeSize/2);
                    break;
                case LEFT:
                    eye1.setLocation(hx + eyeSize/2, hy + eyeSize/2);
                    eye2.setLocation(hx + eyeSize/2, hy + CELL_SIZE - eyeSize - eyeSize/2);
                    break;
                case RIGHT:
                    eye1.setLocation(hx + CELL_SIZE - eyeSize - eyeSize/2, hy + eyeSize/2);
                    eye2.setLocation(hx + CELL_SIZE - eyeSize - eyeSize/2, hy + CELL_SIZE - eyeSize - eyeSize/2);
                    break;
            }
            g2d.fillOval(eye1.x, eye1.y, eyeSize, eyeSize);
            g2d.fillOval(eye2.x, eye2.y, eyeSize, eyeSize);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(eye1.x + eyeSize/4, eye1.y + eyeSize/4, pupilSize, pupilSize);
            g2d.fillOval(eye2.x + eyeSize/4, eye2.y + eyeSize/4, pupilSize, pupilSize);
        }
    }

    private void drawWaitingScreen(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
        String text = "Waiting for game to start...";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2);
    }
    
    private void drawCountdown(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 150));
        g2d.setColor(new Color(255, 255, 255, 200));
        String text = String.valueOf(countdown);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2 + fm.getAscent() / 3);
    }
    
    private void drawGoText(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 150));
        g2d.setColor(new Color(50, 255, 50, 220));
        String text = "GO!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2 + fm.getAscent() / 3);
    }
}

