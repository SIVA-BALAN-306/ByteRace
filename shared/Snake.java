package shared;
import java.io.Serializable;
import java.awt.Point;
import java.util.LinkedList;

public class Snake implements Serializable {
    public LinkedList<Point> body = new LinkedList<>();
    public int headX, headY;
    public Move.Direction dir;

    public Snake(int startX, int startY, Move.Direction direction) {
        headX = startX;
        headY = startY;
        dir = direction;
        body.add(new Point(headX, headY));
    }

    public void move() {
        switch (dir) {
            case UP -> headY--;
            case DOWN -> headY++;
            case LEFT -> headX--;
            case RIGHT -> headX++;
        }
        body.addFirst(new Point(headX, headY));
        body.removeLast();
    }

    public void changeDirection(Move.Direction newDir) {
        // Ignore opposite directions
        if ((dir == Move.Direction.UP && newDir == Move.Direction.DOWN) ||
            (dir == Move.Direction.DOWN && newDir == Move.Direction.UP) ||
            (dir == Move.Direction.LEFT && newDir == Move.Direction.RIGHT) ||
            (dir == Move.Direction.RIGHT && newDir == Move.Direction.LEFT)) {
            return;
        }
        dir = newDir;
    }
}
