package shared;

import java.io.Serializable;

public class Move implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Direction {UP, DOWN, LEFT, RIGHT}
    public Direction direction;

    public Move(Direction direction){
        this.direction = direction;
    }
}


