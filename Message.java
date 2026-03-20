import java.awt.Point;
import java.io.Serializable;
import java.util.Map;

/**
 * A serializable class for communication between the server and clients.
 * This class encapsulates all the data that needs to be sent over the network.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final Object payload;

    public enum MessageType {
        JOIN,          // Client wants to join the game
        MOVE,          // Client sends a move command
        GAME_STATE,    // Server sends the updated game state
        GAME_OVER,     // Server announces the end of the game
        PLAYER_INFO,   // Server sends initial player info (like color)
        ERROR          // Server sends an error message
    }

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     * A static nested class to represent the full game state.
     * This is sent from the server to all clients to update their display.
     */
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Map<Integer, Snake> snakes;
        public final Point food;
        public final Map<Integer, String> playerNames;
        public final Map<Integer, String> playerSymbols;

        public GameState(Map<Integer, Snake> snakes, Point food, Map<Integer, String> playerNames, Map<Integer, String> playerSymbols) {
            this.snakes = snakes;
            this.food = food;
            this.playerNames = playerNames;
            this.playerSymbols = playerSymbols;
        }
    }
}

