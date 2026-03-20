package Server;

import shared.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Color;

public class SnakeServer {

    private static final int PORT = 12345;
    private static final int BOARD_WIDTH = 80;
    private static final int BOARD_HEIGHT = 40;

    private final List<PlayerInfo> PlayerInfos = new ArrayList<>();
    private char[][] board = new char[BOARD_HEIGHT][BOARD_WIDTH];

    public SnakeServer() {
        // Initialize board
        for(int i=0;i<BOARD_HEIGHT;i++)
            Arrays.fill(board[i], '.');

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Snake server running on port " + PORT);

            while (PlayerInfos.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }

            // Start game loop here (timer or loop)
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Request name & symbol
            out.writeObject(new Message(Message.Type.REQUEST_NAME, null));
            out.flush();

            String name = "PlayerInfo";
            String symbol = "S";

            // Wait for response
            Object o = in.readObject();
            if (o instanceof Message msg && msg.type == Message.Type.NAME_RESPONSE) {
                String[] payload = (String[]) msg.payload;
                name = payload[0];
                symbol = payload[1];
            }

            // Spawn snake at center with offset
            int centerX = BOARD_WIDTH / 2;
            int centerY = BOARD_HEIGHT / 2;
            int offset = 4;
            Snake snake;

            synchronized(PlayerInfos){
                if (PlayerInfos.size() == 0)
                    snake = new Snake(centerX - offset, centerY, Move.Direction.RIGHT);
                else
                    snake = new Snake(centerX + offset, centerY, Move.Direction.LEFT);
            }

            PlayerInfo PlayerInfo = new PlayerInfo(name, symbol.charAt(0), snake);

            PlayerInfos.add(PlayerInfo);

            // Assign color randomly
            PlayerInfo.color = new Color(new Random().nextInt(256),
                                     new Random().nextInt(256),
                                     new Random().nextInt(256));

            // Send info assign
            out.writeObject(new Message(Message.Type.INFO_ASSIGN, PlayerInfo));
            out.flush();

            // Listen for moves
            while(true){
                Object obj = in.readObject();
                if(obj instanceof Message m && m.type == Message.Type.MOVE){
                    Move mv = (Move) m.payload;
                    // server logic to handle move (ignore reverse)
                    snake.changeDirection(mv.direction);
                }
            }

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        new SnakeServer();
    }
}
