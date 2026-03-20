package v1.Server;

import java.net.*;
import java.util.*;

import v1.shared.Point;

public class SnakeServer {
    public static void main(String[] args) throws Exception{
        int port = 12345;
        ServerSocket server = new ServerSocket(port);

        // Print server IP
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Server started on IP: " + ip + " Port: " + port);
        System.out.println("Waiting for 2 players...");

        GameState gameState = new GameState();
        List<ClientHandler> clients = new ArrayList<>();

        for(int i=1;i<=2;i++){
            Socket socket = server.accept();
            System.out.println("Player "+i+" connected from "+socket.getInetAddress());
            gameState.addSnake(i, new Point(i*2, i*2));
            ClientHandler handler = new ClientHandler(socket, gameState, i);
            clients.add(handler);
            new Thread(handler).start();
        }

        System.out.println("Both players connected. Game starts!");

        while(true){
            gameState.updateSnakes();

            for(int i=1;i<=2;i++){
                if(gameState.checkCollision(i)){
                    System.out.println("Player "+i+" collided. Game Over!");
                    for(ClientHandler ch: clients) ch.sendBoard("Player "+i+" collided. Game Over!");
                    System.exit(0);
                }
            }

            String board = gameState.renderBoard();
            for(ClientHandler ch: clients) ch.sendBoard(board);
            Thread.sleep(500);
        }
    }
}
