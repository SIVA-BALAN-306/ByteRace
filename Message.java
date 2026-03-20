import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The main communication class. GameState updated to remove symbols.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private Object payload;

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }

    public enum MessageType {
        JOIN, MOVE, GAME_STATE, PLAYER_INFO, YOUR_ID, COUNTDOWN, GAME_OVER, ERROR, BOOST
    }

    public static class GameState implements Serializable {
        private static final long serialVersionUID = 2L;
        public final Map<Integer, Snake> snakes;
        public final List<Food> foodItems;
        public final Map<Integer, String> playerNames;
        // playerSymbols removed
        public final int boardWidth;
        public final int boardHeight;

        public GameState(Map<Integer, Snake> snakes, List<Food> foodItems, Map<Integer, String> playerNames, int boardWidth, int boardHeight) {
            this.snakes = snakes;
            this.foodItems = foodItems;
            this.playerNames = playerNames;
            this.boardWidth = boardWidth;
            this.boardHeight = boardHeight;
        }
    }

    public static class Food implements Serializable {
        private static final long serialVersionUID = 3L;
        public final Point location;
        public final Color color;
        
        public Food(Point location, Color color) {
            this.location = location;
            this.color = color;
        }
    }
}

