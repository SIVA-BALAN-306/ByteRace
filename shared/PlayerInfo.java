package shared;

import java.awt.Color;
import java.io.Serializable;

public class PlayerInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String name;
    public char symbol;
    public Color color; // color for both head/body (we can vary shade client-side)
    public Snake snake;

    public PlayerInfo(String name, char symbol, Snake snake) {
        this.name = name;
        this.symbol = symbol;
        this.snake = snake;
        this.color = new Color((int)(Math.random() * 0x1000000)); // random color
    }
}
