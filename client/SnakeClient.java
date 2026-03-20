package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import shared.Move;
import shared.Move.Direction;

public class SnakeClient {
    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter server IP: ");
        String serverIP = sc.nextLine();

        Socket socket = new Socket(serverIP, 12345); // connect to server over Wi-Fi
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // Thread to receive board updates
        new Thread(() -> {
            try {
                while(true){
                    String board = (String) in.readObject();
                    System.out.println(board);
                }
            } catch(Exception e){ e.printStackTrace(); }
        }).start();

        // Main thread: read user input and send move
        while(true){
            String input = sc.nextLine().toUpperCase();
            Direction dir;
            switch(input){
                case "W": dir = Direction.UP; break;
                case "S": dir = Direction.DOWN; break;
                case "A": dir = Direction.LEFT; break;
                case "D": dir = Direction.RIGHT; break;
                default: continue;
            }
            out.writeObject(new Move(dir));
            out.reset();
        }
    }
}
