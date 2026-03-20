import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Represents a snake in the game.
 * This class holds the snake's body, color, and current direction.
 * It is serializable to be sent as part of the game state message.
 */
public class Snake implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedList<Point> body;
    private Direction direction;
    private final Color color;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Snake(Point startPosition, Color color) {
        this.body = new LinkedList<>();
        this.body.add(startPosition);
        this.body.add(new Point(startPosition.x - 1, startPosition.y)); // Add a second segment
        this.direction = Direction.RIGHT; // Default starting direction
        this.color = color;
    }

    public void move() {
        Point newHead = (Point) getHead().clone();
        switch (direction) {
            case UP:
                newHead.y--;
                break;
            case DOWN:
                newHead.y++;
                break;
            case LEFT:
                newHead.x--;
                break;
            case RIGHT:
                newHead.x++;
                break;
        }
        body.addFirst(newHead); // Add new head
        body.removeLast();      // Remove tail
    }

    public void grow() {
        // The tail from the previous frame is effectively the new segment.
        // We just don't remove the tail in the next move.
        // To do this properly, we add a copy of the current tail.
        body.addLast((Point) body.getLast().clone());
    }

    public Point getHead() {
        return body.getFirst();
    }

    public LinkedList<Point> getBody() {
        return body;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction newDirection) {
        // Prevent the snake from reversing into itself
        if (direction == Direction.UP && newDirection == Direction.DOWN) return;
        if (direction == Direction.DOWN && newDirection == Direction.UP) return;
        if (direction == Direction.LEFT && newDirection == Direction.RIGHT) return;
        if (direction == Direction.RIGHT && newDirection == Direction.LEFT) return;
        this.direction = newDirection;
    }

    public Color getColor() {
        return color;
    }

    // Check if a point (like a food item) is on the snake's body.
    public boolean contains(Point p) {
        return body.contains(p);
    }
}
