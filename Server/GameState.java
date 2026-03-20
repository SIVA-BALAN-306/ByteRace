package v1.Server;

import java.util.*;

import v1.shared.Move;
import v1.shared.Point;

public class GameState {
    public final int WIDTH = 20;
    public final int HEIGHT = 10;

    public Map<Integer, LinkedList<Point>> snakes = new HashMap<>();
    public Map<Integer, Move.Direction> directions = new HashMap<>();
    public Point food;

    private Random rand = new Random();

    public GameState(){
        // Initialize food
        spawnFood();
    }

    public void addSnake(int playerId, Point start){
        LinkedList<Point> snake = new LinkedList<>();
        snake.add(start);
        snakes.put(playerId, snake);
        directions.put(playerId, Move.Direction.RIGHT);
    }

    public void updateSnakes(){
        for(Integer id : snakes.keySet()){
            LinkedList<Point> snake = snakes.get(id);
            Move.Direction dir = directions.get(id);
            Point head = snake.getFirst();
            Point newHead = new Point(head.x, head.y);

            switch(dir){
                case UP -> newHead.y--;
                case DOWN -> newHead.y++;
                case LEFT -> newHead.x--;
                case RIGHT -> newHead.x++;
            }

            snake.addFirst(newHead);

            // Check food
            if(newHead.equals(food)){
                spawnFood(); // grow snake, do not remove tail
            } else {
                snake.removeLast(); // move forward
            }
        }
    }

    public boolean checkCollision(int playerId){
        LinkedList<Point> snake = snakes.get(playerId);
        Point head = snake.getFirst();

        // Wall collision
        if(head.x < 0 || head.x >= WIDTH || head.y < 0 || head.y >= HEIGHT) return true;

        // Self collision
        for(int i=1; i<snake.size(); i++){
            if(head.equals(snake.get(i))) return true;
        }

        // Collision with other snakes
        for(Integer id : snakes.keySet()){
            if(id == playerId) continue;
            for(Point p : snakes.get(id)){
                if(head.equals(p)) return true;
            }
        }
        return false;
    }

    public void spawnFood(){
        food = new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT));
    }

    public String renderBoard(){
        char[][] board = new char[HEIGHT][WIDTH];
        for(char[] row: board) Arrays.fill(row, '.');

        for(Point p : snakes.get(1)) board[p.y][p.x] = '1';
        for(Point p : snakes.get(2)) board[p.y][p.x] = '2';

        board[food.y][food.x] = 'F';

        StringBuilder sb = new StringBuilder();
        for(char[] row: board){
            for(char c: row) sb.append(c).append(' ');
            sb.append('\n');
        }
        return sb.toString();
    }
}
