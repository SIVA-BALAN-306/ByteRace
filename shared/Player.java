package shared;
import java.io.*;
import java.io.Serializable;

public class Player implements Serializable {
    public String name;
    public char symbol;
    public Snake snake;
    public transient ObjectOutputStream out;
    public transient ObjectInputStream in;

    public Player(String name, char symbol, Snake snake, ObjectOutputStream out, ObjectInputStream in) {
        this.name = name;
        this.symbol = symbol;
        this.snake = snake;
        this.out = out;
        this.in = in;
    }
}
