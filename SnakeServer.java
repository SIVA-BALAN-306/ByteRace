import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main server for the multiplayer Snake game.
 * It manages game state, client connections, and game logic.
 */
public class SnakeServer {

    // --- Game Constants ---
    private static final int PORT = 12345;
    private static final int BOARD_WIDTH = 40;
    private static final int BOARD_HEIGHT = 40;
    private static final int GAME_SPEED = 150; // Milliseconds between updates

    // --- Server State ---
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private int nextClientId = 0;

    // --- Game State ---
    private final Map<Integer, Snake> snakes = new ConcurrentHashMap<>();
    private Point food;
    private final Random random = new Random();
    private volatile boolean gameRunning = false;

    public static void main(String[] args) {
        new SnakeServer().startServer();
    }

    /**
     * Starts the server, listens for connections, and manages the game loop.
     */
    public void startServer() {
        System.out.println("Snake Server is starting on port " + PORT);
        new Thread(this::gameLoop).start(); // Start the game loop in a new thread

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = nextClientId++;
                System.out.println("New client connected with ID: " + clientId);
                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                clients.put(clientId, handler);
                clientExecutor.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * The main game loop, which runs periodically to update the game state.
     */
    private void gameLoop() {
        while (true) {
            try {
                Thread.sleep(GAME_SPEED);
                if (gameRunning) {
                    updateGameState();
                    broadcastGameState();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Game loop interrupted.");
            }
        }
    }

    /**
     * Updates the positions of all snakes, checks for collisions, and handles food.
     */
    private synchronized void updateGameState() {
        if (snakes.isEmpty()) {
            gameRunning = false;
            return;
        }
        
        List<Integer> clientsToRemove = new ArrayList<>();

        // Move all snakes first
        for (Snake snake : snakes.values()) {
            snake.move();
        }

        // Check for collisions
        Map<Point, Integer> headPositions = new HashMap<>();
        for (Map.Entry<Integer, Snake> entry : snakes.entrySet()) {
            Point head = entry.getValue().getHead();
            if (headPositions.containsKey(head)) { // Head-on collision
                int otherId = headPositions.get(head);
                clientsToRemove.add(entry.getKey());
                clientsToRemove.add(otherId);
                 System.out.println("Head-on collision between " + entry.getKey() + " and " + otherId);
            } else {
                headPositions.put(head, entry.getKey());
            }
        }

        for (Map.Entry<Integer, Snake> entry : snakes.entrySet()) {
            Integer id = entry.getKey();
            if (clientsToRemove.contains(id)) continue;
            
            Snake snake = entry.getValue();
            Point head = snake.getHead();

            // Wall collision
            if (head.x < 0 || head.x >= BOARD_WIDTH || head.y < 0 || head.y >= BOARD_HEIGHT) {
                clientsToRemove.add(id);
                System.out.println("Player " + id + " hit a wall.");
                continue;
            }

            // Body collision with other snakes
            for (Map.Entry<Integer, Snake> otherEntry : snakes.entrySet()) {
                if (id.equals(otherEntry.getKey())) continue; // Don't check against self

                if (otherEntry.getValue().getBody().contains(head)) {
                    clientsToRemove.add(id);
                    System.out.println("Player " + id + " collided with Player " + otherEntry.getKey() + "'s body.");
                    break;
                }
            }
        }
        
        // Handle eliminations
        if (!clientsToRemove.isEmpty()) {
            String endMessage = "Game Over!";
            if (snakes.size() - clientsToRemove.size() == 1) {
                for (Integer id : snakes.keySet()) {
                    if (!clientsToRemove.contains(id)) {
                        endMessage = "Player " + clients.get(id).playerName + " wins!";
                        break;
                    }
                }
            } else if (snakes.size() - clientsToRemove.size() <= 0) {
                 endMessage = "It's a draw!";
            }

            // Notify all clients about the game outcome
            for (ClientHandler handler : clients.values()) {
                 handler.sendMessage(new Message(Message.MessageType.GAME_OVER, endMessage));
            }
            
            // Wait a moment before resetting
            try { Thread.sleep(3000); } catch(InterruptedException e){}
            
            resetGame();
            return; 
        }

        // Food consumption
        for (Snake snake : snakes.values()) {
            if (snake.getHead().equals(food)) {
                snake.grow();
                spawnFood();
                break; // Only one snake can eat the food per frame
            }
        }
    }

    /**
     * Sends the current game state to all connected clients.
     */
    private void broadcastGameState() {
        Map<Integer, String> playerNames = new HashMap<>();
        Map<Integer, String> playerSymbols = new HashMap<>();
        for (ClientHandler handler : clients.values()) {
            playerNames.put(handler.clientId, handler.playerName);
            playerSymbols.put(handler.clientId, handler.playerSymbol);
        }

        Message.GameState gameState = new Message.GameState(
            Collections.unmodifiableMap(new HashMap<>(snakes)), 
            food, 
            playerNames,
            playerSymbols
        );
        Message message = new Message(Message.MessageType.GAME_STATE, gameState);
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }
    
    /**
     * Spowns food at a random location not occupied by any snake.
     */
    private synchronized void spawnFood() {
        do {
            food = new Point(random.nextInt(BOARD_WIDTH), random.nextInt(BOARD_HEIGHT));
        } while (isOccupied(food));
    }
    
    private boolean isOccupied(Point p) {
        // CORRECTED: Iterate over snake *values*, not keys.
        for (Snake snake : snakes.values()) { 
            if (snake.getBody().contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new player to the game.
     */
    private synchronized void addPlayer(int clientId, String name, String symbol) {
        clients.get(clientId).playerName = name;
        clients.get(clientId).playerSymbol = symbol;
        
        System.out.println("Player " + name + " (" + symbol + ") with ID " + clientId + " has joined.");
        
        if (!gameRunning && clients.size() >= 2) {
            resetGame();
        } else if (gameRunning) {
            // If game is running, add new player in the next round
            clients.get(clientId).sendMessage(new Message(Message.MessageType.GAME_OVER, "Game in progress. Please wait for the next round."));
        } else {
             // Not enough players yet
             broadcastGameState(); // show the joined players
        }
    }

    /**
     * Resets the game state for a new round.
     */
    private synchronized void resetGame() {
        System.out.println("Resetting game for " + clients.size() + " players.");
        snakes.clear();
        int i = 0;
        for (ClientHandler handler : clients.values()) {
            // Improved spawning logic to spread players out
            int spawnX = BOARD_WIDTH / 4 + (i % 2) * (BOARD_WIDTH / 2);
            int spawnY = BOARD_HEIGHT / 4 + (i / 2) * (BOARD_HEIGHT / 2);
            
            Color color = Color.getHSBColor(random.nextFloat(), 0.8f, 0.9f);
            Snake snake = new Snake(new Point(spawnX, spawnY), color);
            snakes.put(handler.clientId, snake);
            handler.sendMessage(new Message(Message.MessageType.PLAYER_INFO, color));
            i++;
        }
        
        spawnFood();
        gameRunning = true;
        System.out.println("Game has started!");
    }

    /**
     * Removes a client from the game, for example on disconnect.
     */
    private synchronized void removeClient(int clientId) {
        ClientHandler removedClient = clients.remove(clientId);
        snakes.remove(clientId);
        if (removedClient != null) {
            System.out.println("Client " + clientId + " ("+ removedClient.playerName +") has been removed.");
        }
        
        if (gameRunning && clients.size() < 2) {
            gameRunning = false;
            System.out.println("Not enough players. Game paused.");
            if(!clients.isEmpty()){
                 ClientHandler winner = clients.values().iterator().next();
                 winner.sendMessage(new Message(Message.MessageType.GAME_OVER, "The other player disconnected. You win!"));
            }
        }
        broadcastGameState(); // Update remaining players
    }

    /**
     * Handles communication with a single client.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        public String playerName;
        public String playerSymbol;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());

                // The first message must be a JOIN message
                Message msg = (Message) ois.readObject();
                if (msg.getType() == Message.MessageType.JOIN) {
                    String[] data = (String[]) msg.getPayload();
                    addPlayer(clientId, data[0], data[1]);
                } else {
                    // Invalid first message
                    System.out.println("Client " + clientId + " sent invalid initial message. Disconnecting.");
                    return;
                }

                while (true) {
                    msg = (Message) ois.readObject();
                    handleMessage(msg);
                }
            } catch (IOException | ClassNotFoundException e) {
                // This is expected when a client disconnects
            } finally {
                removeClient(clientId);
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        private void handleMessage(Message msg) {
            if (msg.getType() == Message.MessageType.MOVE) {
                if (!gameRunning) return; // Ignore moves if game isn't running
                
                Snake.Direction dir = (Snake.Direction) msg.getPayload();
                // We need to synchronize access to the snake object
                synchronized (SnakeServer.this) {
                    Snake snake = snakes.get(clientId);
                    if (snake != null) {
                        snake.setDirection(dir);
                    }
                }
            }
            // Other client-sent message types can be handled here
        }

        public void sendMessage(Message msg) {
            try {
                if (!socket.isClosed()) {
                    // To prevent ConcurrentModificationException, writeObject should be synchronized
                    synchronized(oos) {
                        oos.writeObject(msg);
                        oos.flush();
                        oos.reset(); // Important for sending updated objects
                    }
                }
            } catch (IOException e) {
                System.err.println("Error sending message to client " + clientId + ": " + e.getMessage());
            }
        }
    }
}

