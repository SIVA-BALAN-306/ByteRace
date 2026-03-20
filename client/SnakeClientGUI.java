package client;

import shared.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SnakeClientGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JTextArea infoArea;
    private DrawPanel drawPanel;
    private PlayerInfo myInfo; // assigned by server
    private Map<Integer, PlayerInfo> infos; // all players
    private char[][] latestBoard;

    public SnakeClientGUI(String serverIP) {
        setTitle("Multiplayer Snake (GUI)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        infoArea = new JTextArea(4,20);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(infoArea), BorderLayout.EAST);

        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        setSize(900,600);
        setLocationRelativeTo(null);
        setVisible(true);

        // ensure panel has focus for key events
        drawPanel.setFocusable(true);
        drawPanel.requestFocusInWindow();

        // connect to server
        try {
            Socket socket = new Socket(serverIP, 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // listen thread
            new Thread(() -> {
                try {
                    // handshake and normal loop
                    while(true){
                        Object o = in.readObject();
                        if(!(o instanceof Message)) continue;
                        Message m = (Message) o;
                        switch(m.type){
                            case REQUEST_NAME -> {
                                // prompt user for name and a single char
                                String name = JOptionPane.showInputDialog(this, "Enter your name:");
                                if(name == null || name.trim().isEmpty()) name = "Player";
                                String sym = JOptionPane.showInputDialog(this, "Enter a single character to represent your snake (e.g. S, @, #):");
                                if(sym == null || sym.isEmpty()) sym = "S";
                                if(sym.length()>1) sym = sym.substring(0,1);
                                String[] payload = new String[]{name, sym};
                                out.writeObject(new Message(Message.Type.NAME_RESPONSE, payload));
                                out.reset();
                            }
                            case INFO_ASSIGN -> {
                                myInfo = (PlayerInfo) m.payload;
                                // show assigned
                                infoArea.setText("You: " + myInfo.name + " (" + myInfo.symbol + ")\nColor: " + myInfo.color + "\nWaiting for board...");
                                // focus for keys
                                drawPanel.requestFocusInWindow();
                            }
                            case BOARD_UPDATE -> {
                                @SuppressWarnings("unchecked")
                                Map<String,Object> payload = (Map<String,Object>) m.payload;
                                latestBoard = (char[][]) payload.get("board");
                                @SuppressWarnings("unchecked")
                                Map<Integer, PlayerInfo> pinfo = (Map<Integer, PlayerInfo>) payload.get("infos");
                                infos = pinfo;
                                drawPanel.repaint();
                                updateInfoPanel();
                            }
                            case STATUS -> {
                                String st = (String) m.payload;
                                if(st.equals("DRAW")){
                                    JOptionPane.showMessageDialog(this, "Game Over: DRAW");
                                } else if(st.startsWith("ELIMINATED")){
                                    JOptionPane.showMessageDialog(this, "Game Over: " + st);
                                } else {
                                    JOptionPane.showMessageDialog(this, "Status: " + st);
                                }
                                System.exit(0);
                            }
                            default -> {}
                        }
                    }
                } catch(Exception ex){
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Connection lost.");
                    System.exit(0);
                }
            }).start();

            // Key handling for moves
            drawPanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    try {
                        Move.Direction dir = null;
                        switch(e.getKeyCode()){
                            case KeyEvent.VK_W -> dir = Move.Direction.UP;
                            case KeyEvent.VK_S -> dir = Move.Direction.DOWN;
                            case KeyEvent.VK_A -> dir = Move.Direction.LEFT;
                            case KeyEvent.VK_D -> dir = Move.Direction.RIGHT;
                        }
                        if(dir != null){
                            // send move; server will ignore reverse
                            out.writeObject(new Message(Message.Type.MOVE, new Move(dir)));
                            out.reset();
                        }
                    } catch(IOException ioe){ ioe.printStackTrace(); }
                }
            });

        } catch(IOException ioe){
            JOptionPane.showMessageDialog(this, "Unable to connect to server: " + ioe.getMessage());
            System.exit(0);
        }
    }

    private void updateInfoPanel(){
        StringBuilder sb = new StringBuilder();
        sb.append("Players:\n");
        if(infos != null){
            for(Integer id: infos.keySet()){
                PlayerInfo pi = infos.get(id);
                sb.append("P").append(id).append(": ").append(pi.name)
                  .append(" (").append(pi.symbol).append(") ");
                sb.append(" Color: ").append(pi.color).append("\n");
            }
        }
        infoArea.setText(sb.toString());
    }

    private class DrawPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            // background gradient
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            Color c1 = new Color(34, 47, 62);
            Color c2 = new Color(58, 123, 213);
            GradientPaint gp = new GradientPaint(0,0,c1, w, h, c2);
            g2.setPaint(gp);
            g2.fillRect(0,0,w,h);

            if(latestBoard == null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 24));
                g2.drawString("Waiting for players / game to start...", 30, 50);
                return;
            }

            int rows = latestBoard.length;
            int cols = latestBoard[0].length;

            // compute grid cell size to fit panel
            int pad = 20;
            int gridW = Math.min((w - 200 - pad*2), (h - pad*2)); // leave space for info panel
            int cellSize = Math.max(12, gridW / Math.max(cols, rows));
            int boardW = cellSize * cols;
            int boardH = cellSize * rows;
            int startX = 20;
            int startY = (h - boardH)/2;

            // draw grid background
            g2.setColor(new Color(10,10,10,100));
            g2.fillRoundRect(startX-6, startY-6, boardW+12, boardH+12, 12,12);

            // draw cells
            for(int r=0;r<rows;r++){
                for(int c=0;c<cols;c++){
                    int x = startX + c*cellSize;
                    int y = startY + r*cellSize;
                    g2.setColor(new Color(0,0,0,60));
                    g2.fillRect(x, y, cellSize-1, cellSize-1);

                    char ch = latestBoard[r][c];
                    if(ch == '.') continue;
                    if(ch == 'F'){
                        g2.setColor(Color.YELLOW);
                        int margin = cellSize/6;
                        g2.fillOval(x+margin, y+margin, cellSize-2*margin, cellSize-2*margin);
                        continue;
                    }
                    PlayerInfo p = null;
                    if(infos!=null){
                        for(PlayerInfo pi: infos.values()){
                            if(pi.symbol == ch){ p = pi; break; }
                        }
                    }
                    if(p==null) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(String.valueOf(ch), x + cellSize/3, y + cellSize*2/3);
                    } else {
                        g2.setColor(p.color);
                        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(12, cellSize-4)));
                        g2.drawString(String.valueOf(ch), x + cellSize/4, y + cellSize*3/4);
                    }
                }
            }

            // draw overlay: player key + color
            int infoX = startX + boardW + 20;
            int infoY = startY + 30;
            g2.setColor(new Color(255,255,255,180));
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("Players", infoX, infoY);
            infoY += 20;
            if(infos != null){
                for(Integer id: infos.keySet()){
                    PlayerInfo pi = infos.get(id);
                    g2.setColor(pi.color);
                    g2.fillRect(infoX, infoY, 24, 24);
                    g2.setColor(Color.WHITE);
                    g2.drawRect(infoX, infoY, 24,24);
                    g2.drawString("  P" + id + ": " + pi.name + " (" + pi.symbol + ")", infoX + 32, infoY + 17);
                    infoY += 34;
                }
            }
        }
    }

    public static void main(String[] args){
        final String serverIP = JOptionPane.showInputDialog("Enter server IP (or localhost):");
        SwingUtilities.invokeLater(() -> new SnakeClientGUI(
            (serverIP == null || serverIP.isEmpty()) ? "localhost" : serverIP)
        );
    }
}
