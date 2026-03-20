import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A serializable class for all communication between the server and clients.
 * It uses an enum to define the message type and a payload for the data.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        JOIN,          // Client -> Server: Player joining
        MOVE,          // Client -> Server: Player wants to move
        BOOST,         // Client -> Server: Player wants to boost
        GAME_STATE,    // Server -> Client: The current state of the game
        YOUR_ID,       // Server -> Client: The ID assigned to this client
        PLAYER_INFO,   // Server -> Client: Your snake's color
        COUNTDOWN,     // Server -> Client: Game start countdown
        GAME_OVER,     // Server -> Client: Game has ended
        ERROR          // Server -> Client: An error occurred
    }

    private final MessageType type;
    private final Object payload;

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
     * A container for sending the entire game state at once.
     */
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Map<Integer, Snake> snakes;
        public final List<Food> foodItems;
        public final Map<Integer, String> playerNames;
        public final Map<Integer, String> playerSymbols;
        public final int boardWidth;
        public final int boardHeight;

        public GameState(Map<Integer, Snake> snakes, List<Food> foodItems, Map<Integer, String> playerNames, Map<Integer, String> playerSymbols, int boardWidth, int boardHeight) {
            this.snakes = snakes;
            this.foodItems = foodItems;
            this.playerNames = playerNames;
            this.playerSymbols = playerSymbols;
            this.boardWidth = boardWidth;
            this.boardHeight = boardHeight;
        }
    }

    /**
     * A simple container for food data.
     */
    public static class Food implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Point location;
        public final Color color;

        public Food(Point location, Color color) {
            this.location = location;
            this.color = color;
        }
    }
}

