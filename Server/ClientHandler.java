package v1.Server;

import java.io.*;
import java.net.Socket;

import v1.shared.Move;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameState gameState;
    private int playerId;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, GameState gameState, int playerId){
        this.socket = socket;
        this.gameState = gameState;
        this.playerId = playerId;
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch(IOException e){ e.printStackTrace(); }
    }

    public void run(){
        try{
            while(true){
                Move move = (Move) in.readObject();
                gameState.directions.put(playerId, move.direction);
            }
        } catch(Exception e){
            System.out.println("Player "+playerId+" disconnected.");
        }
    }

    public void sendBoard(String board){
        try{
            out.writeObject(board);
            out.reset();
        } catch(IOException e){ e.printStackTrace(); }
    }
}
