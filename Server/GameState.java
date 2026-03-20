package Server;

import shared.PlayerInfo;
import shared.Move;
import shared.Message;

import java.awt.Point;
import java.util.*;
import java.io.Serializable;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int WIDTH = 24;
    public final int HEIGHT = 16;

    // map playerId -> snake body (head at index 0)
    public Map<Integer, LinkedList<Point>> snakes = new HashMap<>();
    // map playerId -> direction
    public Map<Integer, Move.Direction> directions = new HashMap<>();
    // map playerId -> PlayerInfo
    public Map<Integer, PlayerInfo> infos = new HashMap<>();
    public Point food;
    private Random rand = new Random();

    public GameState(){ spawnFood(); }

    public void addPlayer(int id, PlayerInfo info, Point start){
        LinkedList<Point> body = new LinkedList<>();
        body.add(start);
        snakes.put(id, body);
        // default direction random (not opposite issue)
        directions.put(id, Move.Direction.RIGHT);
        infos.put(id, info);
    }

    public void spawnFood(){
        while(true){
            Point p = new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT));
            // ensure not on any snake
            boolean ok = true;
            for (LinkedList<Point> s : snakes.values()){
                for(Point seg: s) if(seg.equals(p)) { ok = false; break; }
                if(!ok) break;
            }
            if(ok){ food = p; break; }
        }
    }

    public void updateSnakes(){
        // move heads
        Map<Integer, Point> newHeads = new HashMap<>();
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
            newHeads.put(id, newHead);
        }

        // add new heads and remove tail unless eating food
        for(Integer id : snakes.keySet()){
            LinkedList<Point> snake = snakes.get(id);
            Point nh = newHeads.get(id);
            snake.addFirst(nh);
            if(nh.equals(food)){
                spawnFood(); // grow
            } else {
                snake.removeLast();
            }
        }
    }

    // returns status string; "OK", "ELIMINATED:playerId", "DRAW", "ALL_ELIMINATED"
    public String checkCollisions(){
        Set<Integer> eliminated = new HashSet<>();
        // wall collision & head-head detection
        Map<Integer, Point> heads = new HashMap<>();
        for(Integer id : snakes.keySet()){
            Point h = snakes.get(id).getFirst();
            heads.put(id, h);
            if(h.x < 0 || h.x >= WIDTH || h.y < 0 || h.y >= HEIGHT){
                eliminated.add(id); // wall hit
            }
        }

        // head-head collision: if two heads equal -> draw
        List<Integer> ids = new ArrayList<>(heads.keySet());
        for(int i=0;i<ids.size();i++){
            for(int j=i+1;j<ids.size();j++){
                if(heads.get(ids.get(i)).equals(heads.get(ids.get(j)))){
                    return "DRAW";
                }
            }
        }

        // head hitting other player's body
        for(Integer id : snakes.keySet()){
            Point head = heads.get(id);
            for(Integer other : snakes.keySet()){
                if(other.equals(id)) continue; // self-overlap allowed
                LinkedList<Point> otherBody = snakes.get(other);
                // check against all segments of other player's body
                for(Point seg : otherBody){
                    if(head.equals(seg)){
                        eliminated.add(id);
                        break;
                    }
                }
            }
        }

        if(eliminated.isEmpty()) return "OK";
        // if both eliminated -> draw
        if(eliminated.size() == snakes.size()) return "DRAW";
        // if exactly one eliminated and other alive -> indicate which
        if(eliminated.size() == 1){
            int eliminatedId = eliminated.iterator().next();
            return "ELIMINATED:" + eliminatedId;
        }
        // multiple eliminated but not all -> return eliminated list as comma separated
        StringBuilder sb = new StringBuilder("ELIMINATED:");
        int i=0;
        for(Integer e: eliminated){
            if(i++>0) sb.append(",");
            sb.append(e);
        }
        return sb.toString();
    }

    // For sending a simple board representation to clients:
    // We'll send a char grid where '.' empty, 'F' food, and symbol char for snakes
    public char[][] renderBoardSymbols(){
        char[][] board = new char[HEIGHT][WIDTH];
        for(int y=0;y<HEIGHT;y++) for(int x=0;x<WIDTH;x++) board[y][x] = '.';
        // food
        if(food != null) board[food.y][food.x] = 'F';
        // snakes: place their char (head & body same symbol)
        for(Integer id : snakes.keySet()){
            PlayerInfo info = infos.get(id);
            LinkedList<Point> body = snakes.get(id);
            for(Point p : body){
                if(p.x>=0 && p.x<WIDTH && p.y>=0 && p.y<HEIGHT) board[p.y][p.x] = info.symbol;
            }
        }
        return board;
    }
}
