import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Represents a snake in the game with new features.
 * This class now holds the snake's score and power-up state.
 * It is serializable to be sent as part of the game state message.
 */
public class Snake implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedList<Point> body;
    private Direction direction;
    private final Color color;
    
    // New Features
    public int score;
    public int powerUpMeter; // 0-100
    public boolean isBoosting;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Snake(Point startPosition, Color color) {
        this.body = new LinkedList<>();
        this.body.add(startPosition);
        this.body.add(new Point(startPosition.x - 1, startPosition.y)); // Add a second segment
        this.direction = Direction.RIGHT; // Default starting direction
        this.color = color;
        this.score = 0;
        this.powerUpMeter = 0;
        this.isBoosting = false;
    }

    public void move() {
        Point newHead = (Point) getHead().clone();
        switch (direction) {
            case UP:    newHead.y--; break;
            case DOWN:  newHead.y++; break;
            case LEFT:  newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }
        body.addFirst(newHead); // Add new head
        // The grow() method now adds a tail segment, so we always remove the last one here.
        body.removeLast();      
    }

    public void grow() {
        // Add a new segment at the tail's position before the next move.
        body.addLast((Point) body.getLast().clone());
        this.score += 10; // Increase score for eating
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
        if (body.size() > 1) {
            if (direction == Direction.UP && newDirection == Direction.DOWN) return;
            if (direction == Direction.DOWN && newDirection == Direction.UP) return;
            if (direction == Direction.LEFT && newDirection == Direction.RIGHT) return;
            if (direction == Direction.RIGHT && newDirection == Direction.LEFT) return;
        }
        this.direction = newDirection;
    }

    public Color getColor() {
        return color;
    }

    public boolean contains(Point p) {
        return body.contains(p);
    }
}

