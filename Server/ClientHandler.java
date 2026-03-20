package Server;

import shared.*;
import java.net.*;
import java.io.*;
import java.awt.Point;

public class ClientHandler implements Runnable {
    private Socket socket;
    private SnakeServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private PlayerInfo playerInfo;

    public ClientHandler(Socket socket, SnakeServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 1) Ask player for name & symbol
            out.writeObject(new Message(Message.Type.REQUEST_NAME, null));
            out.flush();

            Object obj = in.readObject();
            if (!(obj instanceof Message)) return;
            Message m = (Message) obj;

            String[] payload = (String[]) m.payload;
            String name = payload[0];
            char symbol = payload[1].charAt(0); // only single char

            // 2) Assign spawn location from GameState
            // Replace with actual spawn method in your GameState
            Point spawnPoint = server.getSpawnPoint(playerId); 
            Move.Direction dir = Move.Direction.RIGHT; // default direction

            Snake playerSnake = new Snake(spawnPoint.x, spawnPoint.y, dir);

            // 3) Create PlayerInfo
            playerInfo = new PlayerInfo(name, symbol, playerSnake);

            // 4) Add player to GameState
            server.getGameState().addPlayer(playerId, playerInfo, spawnPoint);

            // 5) Send info assignment back to client
            out.writeObject(new Message(Message.Type.INFO_ASSIGN, playerInfo));
            out.flush();

            // 6) Main loop to receive moves
            while (true) {
                Object incoming = in.readObject();
                if (!(incoming instanceof Message)) continue;
                Message msg = (Message) incoming;
                if (msg.type == Message.Type.MOVE) {
                    Move mv = (Move) msg.payload;
                    playerSnake.changeDirection(mv.direction);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Remove player from GameState if player leaves
            server.getGameState().removePlayerIfExists(playerId);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void sendMessage(Message m) {
        try {
            out.writeObject(m);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
