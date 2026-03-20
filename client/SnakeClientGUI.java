package client;

import shared.Move;
import shared.Move.Direction;
import shared.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class SnakeClientGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JTextArea boardArea;
    private String serverIP;

    public SnakeClientGUI(String serverIP) {
        this.serverIP = serverIP;
        setTitle("Multiplayer Snake Client");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        boardArea = new JTextArea();
        boardArea.setFont(new Font("Monospaced", Font.BOLD, 20));
        boardArea.setEditable(false);
        add(new JScrollPane(boardArea));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Direction dir = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> dir = Direction.UP;
                    case KeyEvent.VK_S -> dir = Direction.DOWN;
                    case KeyEvent.VK_A -> dir = Direction.LEFT;
                    case KeyEvent.VK_D -> dir = Direction.RIGHT;
                }
                if (dir != null) sendMove(dir);
            }
        });

        setVisible(true);
        requestFocus(); // ensure key listener works

        startConnection();
    }

    private void startConnection() {
        try {
            Socket socket = new Socket(serverIP, 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Thread to receive board updates
            new Thread(() -> {
                try {
                    while (true) {
                        String board = (String) in.readObject();
                        boardArea.setText(board);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Connection lost!");
                    System.exit(0);
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!");
            System.exit(0);
        }
    }

    private void sendMove(Direction dir) {
        try {
            out.writeObject(new Move(dir));
            out.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Enter server IP (or localhost):");
        new SnakeClientGUI(serverIP);
    }
}
