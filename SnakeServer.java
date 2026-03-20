import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The main server, updated with new collision logic and game-ending conditions.
 */
public class SnakeServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 10;
    private static final int GAME_TICK_MS = 100;
    private static final int POWER_UP_MAX = 100;
    private static final int POWER_UP_INCREMENT_PER_TICK = 1;
    private static final int POWER_UP_DECREMENT_PER_BOOST_TICK = 3;
    
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private int nextClientId = 0;

    private final Map<Integer, Snake> snakes = new ConcurrentHashMap<>();
    private final List<Message.Food> foodItems = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private volatile boolean gameRunning = false;
    private final List<Color> availableColors;
    private final List<Color> usedColors = new ArrayList<>();

    private int currentBoardWidth = 60;
    private int currentBoardHeight = 60;

    public SnakeServer() {
        availableColors = new ArrayList<>(Arrays.asList(
            new Color(255, 50, 50), new Color(50, 255, 50), new Color(50, 150, 255),
            new Color(255, 255, 50), new Color(255, 50, 255), new Color(50, 255, 255),
            new Color(255, 150, 50), new Color(150, 50, 255), new Color(170, 255, 50),
            new Color(255, 200, 200)
        ));
        Collections.shuffle(availableColors);
    }

    public static void main(String[] args) {
        new SnakeServer().startServer();
    }

    public void startServer() {
        System.out.println("Enhanced Snake Server is starting on port " + PORT);
        new Thread(this::gameLoop).start();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                if (clients.size() >= MAX_PLAYERS) continue;
                Socket clientSocket = serverSocket.accept();
                int clientId = nextClientId++;
                System.out.println("New client connected, assigning ID: " + clientId);
                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                clients.put(clientId, handler);
                clientExecutor.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gameLoop() {
        while (true) {
            try {
                Thread.sleep(GAME_TICK_MS);
                if (gameRunning) {
                    updateGameState();
                    broadcastGameState();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void updateGameState() {
        if (!gameRunning) return;

        // Update power-ups and move snakes
        snakes.values().forEach(snake -> {
            if (snake.isBoosting) {
                snake.powerUpMeter = Math.max(0, snake.powerUpMeter - POWER_UP_DECREMENT_PER_BOOST_TICK);
                if (snake.powerUpMeter == 0) snake.isBoosting = false;
            } else {
                snake.powerUpMeter = Math.min(POWER_UP_MAX, snake.powerUpMeter + POWER_UP_INCREMENT_PER_TICK);
            }
            snake.move();
            if (snake.isBoosting) snake.move(); // Double move for boost
        });

        // --- New Collision Logic ---
        List<Integer> clientsToRemove = new ArrayList<>();
        Map<Point, List<Integer>> headPositions = new HashMap<>();

        for (Map.Entry<Integer, Snake> entry : snakes.entrySet()) {
            Point head = entry.getValue().getHead();
            headPositions.computeIfAbsent(head, k -> new ArrayList<>()).add(entry.getKey());

            // Wall collision
            if (head.x < 0 || head.x >= currentBoardWidth || head.y < 0 || head.y >= currentBoardHeight) {
                clientsToRemove.add(entry.getKey());
                continue;
            }

            // Collision with OTHER snakes' bodies
            for (Map.Entry<Integer, Snake> otherEntry : snakes.entrySet()) {
                if (entry.getKey().equals(otherEntry.getKey())) continue; // Can't collide with self
                if (otherEntry.getValue().getBody().subList(1, otherEntry.getValue().getBody().size()).contains(head)) {
                    clientsToRemove.add(entry.getKey());
                    break;
                }
            }
        }

        // Check for head-on collisions
        for (List<Integer> ids : headPositions.values()) {
            if (ids.size() > 1) {
                ids.forEach(id -> clientsToRemove.add(id));
            }
        }
        
        // --- Elimination and Tie-Breaker Logic ---
        Set<Integer> distinctClientsToRemove = clientsToRemove.stream().collect(Collectors.toSet());
        if (!distinctClientsToRemove.isEmpty()) {
            // Special case: last two players collide head-on
            if (snakes.size() == 2 && distinctClientsToRemove.size() == 2) {
                List<Integer> finalPlayers = new ArrayList<>(distinctClientsToRemove);
                int p1Id = finalPlayers.get(0);
                int p2Id = finalPlayers.get(1);
                Snake s1 = snakes.get(p1Id);
                Snake s2 = snakes.get(p2Id);
                
                String p1Msg, p2Msg;
                if (s1.score > s2.score) {
                    p1Msg = "You won the tie-breaker with a higher score!";
                    p2Msg = "You lost the tie-breaker on score.";
                } else if (s2.score > s1.score) {
                    p2Msg = "You won the tie-breaker with a higher score!";
                    p1Msg = "You lost the tie-breaker on score.";
                } else {
                    p1Msg = "It's a draw!";
                    p2Msg = "It's a draw!";
                }
                
                endGameForPlayer(p1Id, p1Msg);
                endGameForPlayer(p2Id, p2Msg);
                snakes.clear(); // End game
            } else {
                // Regular elimination
                distinctClientsToRemove.forEach(id -> eliminatePlayer(id, "You have been eliminated!"));
            }
        }

        // --- Win Condition Check ---
        if (gameRunning && snakes.size() < 2) {
            if (snakes.size() == 1) {
                int winnerId = snakes.keySet().iterator().next();
                endGameForPlayer(winnerId, "You are the last one standing! You win!");
            }
            snakes.clear(); // End game
            gameRunning = false;
        }

        // Food consumption
        for (Snake snake : snakes.values()) {
            Message.Food eatenFood = null;
            for (Message.Food f : foodItems) {
                if (snake.getHead().equals(f.location)) {
                    snake.grow();
                    eatenFood = f;
                    break;
                }
            }
            if (eatenFood != null) foodItems.remove(eatenFood);
        }
        
        manageFood();
    }

    private void manageFood() {
        while (foodItems.size() < snakes.size() * 3 && !snakes.isEmpty()) {
            Point foodLocation;
            do {
                foodLocation = new Point(random.nextInt(currentBoardWidth), random.nextInt(currentBoardHeight));
            } while (isOccupied(foodLocation));
            Color foodColor = availableColors.get(random.nextInt(availableColors.size()));
            foodItems.add(new Message.Food(foodLocation, foodColor));
        }
    }

    private void eliminatePlayer(int clientId, String reason) {
        endGameForPlayer(clientId, reason);
        snakes.remove(clientId);
    }
    
    private void endGameForPlayer(int clientId, String reason) {
        ClientHandler handler = clients.get(clientId);
        if (handler != null) {
            handler.sendMessage(new Message(Message.MessageType.GAME_OVER, reason));
        }
        Snake removedSnake = snakes.get(clientId); // Get before removing
        if (removedSnake != null) {
            Color color = removedSnake.getColor();
            usedColors.remove(color);
            availableColors.add(color);
            System.out.println("Player " + clientId + " has finished the game. Reason: " + reason);
        }
    }

    private boolean isOccupied(Point p) {
        return snakes.values().stream().anyMatch(s -> s.contains(p));
    }

    private void broadcastGameState() {
        if (!gameRunning) return;
        Map<Integer, String> playerNames = clients.values().stream()
            .filter(c -> c.playerName != null).collect(Collectors.toMap(c -> c.clientId, c -> c.playerName));
        Map<Integer, String> playerSymbols = clients.values().stream()
            .filter(c -> c.playerSymbol != null).collect(Collectors.toMap(c -> c.clientId, c -> c.playerSymbol));

        Message.GameState gameState = new Message.GameState(new HashMap<>(snakes), new ArrayList<>(foodItems), playerNames, playerSymbols, currentBoardWidth, currentBoardHeight);
        Message message = new Message(Message.MessageType.GAME_STATE, gameState);
        clients.values().forEach(c -> c.sendMessage(message));
    }

    private synchronized void addPlayer(int clientId, String name, String symbol) {
        ClientHandler handler = clients.get(clientId);
        handler.playerName = name;
        handler.playerSymbol = symbol;
        handler.sendMessage(new Message(Message.MessageType.YOUR_ID, clientId));
        System.out.println("Player " + name + " (" + symbol + ") with ID " + clientId + " has joined.");

        if (gameRunning) {
            calculateAndSetBoardSize();
            spawnSnakeForPlayer(clientId);
        } else if (clients.size() >= 2) {
            startGameWithCountdown();
        } else {
            broadcastGameState();
        }
    }

    private synchronized void spawnSnakeForPlayer(int clientId) {
        if (availableColors.isEmpty()) {
            clients.get(clientId).sendMessage(new Message(Message.MessageType.ERROR, "Server is full."));
            return;
        }
        
        Point spawnPoint;
        do {
            spawnPoint = new Point(random.nextInt(currentBoardWidth), random.nextInt(currentBoardHeight));
        } while(isOccupied(spawnPoint));

        Color color = availableColors.remove(0);
        usedColors.add(color);
        Snake snake = new Snake(spawnPoint, color);
        snakes.put(clientId, snake);
        clients.get(clientId).sendMessage(new Message(Message.MessageType.PLAYER_INFO, color));
    }

    private void startGameWithCountdown() {
        new Thread(() -> {
            synchronized(this) {
                if (gameRunning) return;
                
                calculateAndSetBoardSize();

                System.out.println("Game starting with countdown...");
                for (int i = 3; i > 0; i--) {
                    final int count = i;
                    clients.values().forEach(c -> c.sendMessage(new Message(Message.MessageType.COUNTDOWN, count)));
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
                clients.values().forEach(c -> c.sendMessage(new Message(Message.MessageType.COUNTDOWN, 0)));
            
                clients.keySet().forEach(this::spawnSnakeForPlayer);
                manageFood();
                
                gameRunning = true;
                System.out.println("Game has started!");
            }
        }).start();
    }
    
    private synchronized void calculateAndSetBoardSize() {
        int playerCount = clients.size();
        int idealSize = 60 + Math.max(0, playerCount - 2) * 10;
        
        int requiredWidth = 0, requiredHeight = 0;
        if (!snakes.isEmpty()) {
            for (Snake snake : snakes.values()) {
                for (Point p : snake.getBody()) {
                    if (p.x >= requiredWidth)  requiredWidth = p.x + 5;
                    if (p.y >= requiredHeight) requiredHeight = p.y + 5;
                }
            }
        }
        int finalWidth = Math.min(150, Math.max(idealSize, requiredWidth));
        int finalHeight = Math.min(150, Math.max(idealSize, requiredHeight));
        
        if (this.currentBoardWidth != finalWidth || this.currentBoardHeight != finalHeight) {
            this.currentBoardWidth = finalWidth;
            this.currentBoardHeight = finalHeight;
            System.out.println("Board size safely adjusted to: " + currentBoardWidth + "x" + currentBoardHeight + " for " + playerCount + " players.");
        }
    }

    private synchronized void removeClient(int clientId) {
        snakes.remove(clientId); // Remove snake on disconnect
        clients.remove(clientId);
        System.out.println("Client " + clientId + " has disconnected.");
        if (gameRunning) calculateAndSetBoardSize();
    }
    
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        public String playerName, playerSymbol;

        public ClientHandler(Socket socket, int clientId) { this.socket = socket; this.clientId = clientId; }

        @Override
        public void run() {
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) ois.readObject();
                if (msg.getType() == Message.MessageType.JOIN) {
                    String[] data = (String[]) msg.getPayload();
                    addPlayer(clientId, data[0], data[1]);
                } else return;
                while (true) {
                    handleMessage((Message) ois.readObject());
                }
            } catch (Exception e) {
            } finally {
                removeClient(clientId);
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void handleMessage(Message msg) {
            Snake snake = snakes.get(clientId);
            if (snake == null) return;
            switch (msg.getType()) {
                case MOVE: snake.setDirection((Snake.Direction) msg.getPayload()); break;
                case BOOST:
                    boolean boosting = (boolean) msg.getPayload();
                    if (boosting && snake.powerUpMeter > 10) snake.isBoosting = true;
                    else if (!boosting) snake.isBoosting = false;
                    break;
                default: break;
            }
        }

        public void sendMessage(Message msg) {
            try {
                if (!socket.isClosed()) {
                    synchronized(oos) {
                        oos.writeObject(msg); oos.flush(); oos.reset();
                    }
                }
            } catch (IOException e) {}
        }
    }
}

