import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * The client GUI for the multiplayer Snake game.
 * It connects to the server, renders the game, and handles user input.
 */
public class SnakeClientGUI extends JFrame {

    private GamePanel gamePanel;
    private JTextArea playerInfoPanel;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Socket socket;
    private Color playerColor = Color.GRAY; // Default color

    public SnakeClientGUI() {
        setTitle("Multiplayer Snake");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        
        playerInfoPanel = new JTextArea("Players:\n");
        playerInfoPanel.setEditable(false);
        playerInfoPanel.setPreferredSize(new Dimension(200, 0));
        playerInfoPanel.setFont(new Font("Monospaced", Font.BOLD, 14));
        playerInfoPanel.setBackground(Color.LIGHT_GRAY);
        JScrollPane scrollPane = new JScrollPane(playerInfoPanel);
        add(scrollPane, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        setupKeyListener();
        
        // Handle window closing to disconnect gracefully
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ioException) {
                    // ignore
                }
                System.exit(0);
            }
        });
    }

    private void setupKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Snake.Direction dir = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        dir = Snake.Direction.UP;
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        dir = Snake.Direction.DOWN;
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        dir = Snake.Direction.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        dir = Snake.Direction.RIGHT;
                        break;
                }
                if (dir != null) {
                    sendMessage(new Message(Message.MessageType.MOVE, dir));
                }
            }
        });
        setFocusable(true); // JFrame must be focusable to receive key events
    }

    private void connectAndListen() {
        // --- Connection Dialog ---
        JTextField serverAddressField = new JTextField("localhost");
        JTextField playerNameField = new JTextField("Player" + (int)(Math.random() * 100));
        JTextField playerSymbolField = new JTextField("S");
        Object[] message = {
            "Server Address:", serverAddressField,
            "Player Name:", playerNameField,
            "Player Symbol (1 char):", playerSymbolField
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Connect to Server", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }
        
        String serverAddress = serverAddressField.getText();
        String playerName = playerNameField.getText();
        String playerSymbol = playerSymbolField.getText().trim();
        if(playerSymbol.isEmpty()) playerSymbol = "O";
        if(playerSymbol.length() > 1) playerSymbol = playerSymbol.substring(0, 1);
        
        // --- Establish Connection ---
        try {
            socket = new Socket(serverAddress, 12345);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            // Send JOIN message
            sendMessage(new Message(Message.MessageType.JOIN, new String[]{playerName, playerSymbol}));

            // --- Start Listening Thread ---
            new Thread(this::listenToServer).start();

        } catch (UnknownHostException e) {
            showErrorDialog("Server not found: " + e.getMessage());
        } catch (IOException e) {
            showErrorDialog("Could not connect to server: " + e.getMessage());
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
                // Use SwingUtilities to update GUI from this thread
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!socket.isClosed()) {
                SwingUtilities.invokeLater(() -> showErrorDialog("Lost connection to the server."));
            }
        } finally {
            try {
                if(socket != null) socket.close();
            } catch (IOException e) { /* ignore */ }
        }
    }
    
    private void handleServerMessage(Message msg) {
        switch(msg.getType()) {
            case GAME_STATE:
                Message.GameState gs = (Message.GameState) msg.getPayload();
                gamePanel.updateState(gs);
                updatePlayerInfo(gs);
                break;
            case PLAYER_INFO:
                this.playerColor = (Color) msg.getPayload();
                setTitle("Multiplayer Snake - Your color is " + colorToString(playerColor));
                break;
            case GAME_OVER:
                JOptionPane.showMessageDialog(this, msg.getPayload().toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
                break;
            case ERROR:
                 showErrorDialog("Server error: " + msg.getPayload().toString());
                 break;
        }
    }

    private void sendMessage(Message msg) {
        try {
            if (oos != null) {
                oos.writeObject(msg);
                oos.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
    
     private void updatePlayerInfo(Message.GameState gs) {
        StringBuilder info = new StringBuilder("Players:\n\n");
        for (Integer id : gs.playerNames.keySet()) {
            Snake snake = gs.snakes.get(id);
            if(snake == null) continue;
            
            Color c = snake.getColor();
            String colorName = colorToString(c);
            info.append(String.format("%-10s [%s] %s\n", gs.playerNames.get(id), gs.playerSymbols.get(id), colorName));
        }
        playerInfoPanel.setText(info.toString());
    }
    
    private String colorToString(Color c){
        if (c.equals(Color.RED)) return "Red";
        if (c.equals(Color.GREEN)) return "Green";
        if (c.equals(Color.BLUE)) return "Blue";
        if (c.equals(Color.YELLOW)) return "Yellow";
        if (c.equals(Color.ORANGE)) return "Orange";
        if (c.equals(Color.CYAN)) return "Cyan";
        if (c.equals(Color.MAGENTA)) return "Magenta";
        
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float hue = hsb[0] * 360;

        if (hue < 30) return "Red";
        if (hue < 90) return "Yellow";
        if (hue < 150) return "Green";
        if (hue < 210) return "Cyan";
        if (hue < 270) return "Blue";
        if (hue < 330) return "Magenta";
        return "Red";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SnakeClientGUI client = new SnakeClientGUI();
            client.connectAndListen();
        });
    }
}

/**
 * The panel that draws the game board, snakes, and food.
 */
class GamePanel extends JPanel {
    private Message.GameState gameState;
    private static final int PREFERRED_GRID_SIZE = 800;

    GamePanel() {
        setPreferredSize(new Dimension(PREFERRED_GRID_SIZE, PREFERRED_GRID_SIZE));
        setBackground(Color.BLACK);
        this.gameState = new Message.GameState(Collections.emptyMap(), null, Collections.emptyMap(), Collections.emptyMap());
    }

    public void updateState(Message.GameState newState) {
        this.gameState = newState;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        
        // Use a gradient for the background
        int width = getWidth();
        int height = getHeight();
        Color color1 = new Color(20, 20, 40);
        Color color2 = new Color(40, 20, 20);
        GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        if (gameState == null) return;
        
        int boardWidth = 40;
        int boardHeight = 40;

        int cellWidth = getWidth() / boardWidth;
        int cellHeight = getHeight() / boardHeight;

        // Draw food
        if (gameState.food != null) {
            g.setColor(Color.RED);
            g.fillOval(gameState.food.x * cellWidth, gameState.food.y * cellHeight, cellWidth, cellHeight);
        }

        // Draw snakes
        for (Map.Entry<Integer, Snake> entry : gameState.snakes.entrySet()) {
            Snake snake = entry.getValue();
            Color snakeColor = snake.getColor();
            String symbol = gameState.playerSymbols.get(entry.getKey());

            // Draw body
            g.setColor(snakeColor.darker());
            LinkedList<Point> body = snake.getBody();
            for (int i = 1; i < body.size(); i++) {
                Point p = body.get(i);
                g.fillRect(p.x * cellWidth, p.y * cellHeight, cellWidth, cellHeight);
            }

            // Draw head
            Point head = snake.getHead();
            g.setColor(snakeColor);
            g.fillRect(head.x * cellWidth, head.y * cellHeight, cellWidth, cellHeight);
            
            // Draw symbol on head
             g.setColor(Color.WHITE);
             g.setFont(new Font("Arial", Font.BOLD, cellWidth - 2));
             FontMetrics fm = g.getFontMetrics();
             int stringWidth = fm.stringWidth(symbol);
             int stringHeight = fm.getAscent();
             g.drawString(symbol, head.x * cellWidth + (cellWidth - stringWidth)/2, head.y * cellHeight + (cellHeight + stringHeight)/2 - fm.getDescent() +1);
        }
    }
}
