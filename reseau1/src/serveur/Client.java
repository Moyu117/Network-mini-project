package serveur;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class Client extends JFrame {
    // RÃ©seau
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // UI jeu
    private final BoardPanel myBoard = new BoardPanel(true);
    private final BoardPanel enemyBoard = new BoardPanel(false);
    private final JLabel status = new JLabel("Bienvenue");
    private final JButton rotateBtn = new JButton("Orientation : D (droite)");
    private final JButton resetBtn  = new JButton("RÃ©initialiser lâ€™emplacement");
    private final JButton readyBtn  = new JButton("Terminer et prÃªt");

    // UI chat
    private final JTextArea chatArea   = new JTextArea(10, 20);
    private final JTextField chatInput = new JTextField();
    private final JButton chatSend     = new JButton("Envoyer");

    // Etat
    private int boardSize = 10;
    private boolean inPlacement = true;
    private boolean yourTurn = false;
    private char orientation = 'D';
    private int[] fleet = {5,4,3,3,2};
    private int fleetIndex = 0;
    private int placedCount = 0;
    private boolean serverPlacementComplete = false;

    private boolean placementPending = false;
    private int pendX, pendY, pendSize;
    private char pendOri;

    private String chosenMode = "PVP";

    public Client() {
        super("Bataille Navale - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Barre supérieure
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(status);
        top.add(rotateBtn);
        top.add(resetBtn);
        top.add(readyBtn);
        add(top, BorderLayout.NORTH);

        rotateBtn.addActionListener(e -> {
            orientation = switch (orientation) {
                case 'D' -> 'B';
                case 'B' -> 'G';
                case 'G' -> 'H';
                default -> 'D';
            };
            rotateBtn.setText("Orientation : " + orientationDesc(orientation));
        });

        resetBtn.addActionListener(e -> {
            if (!inPlacement) return;
            myBoard.clear();
            enemyBoard.clear();
            fleetIndex = 0;
            placedCount = 0;
            serverPlacementComplete = false;
            placementPending = false;
            status.setText("Placement reinitialise. Placez le premier navire.");
            if (out != null) out.println("RESET");
        });

        readyBtn.addActionListener(e -> {
            if (inPlacement && (fleetIndex >= fleet.length || serverPlacementComplete)) out.println("READY");
            else status.setText("Il y a encore des navires non places.");
        });

        // Centre : plateaux + chat
        JPanel boards = new JPanel(new GridLayout(1, 2, 12, 0));
        boards.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        boards.add(wrapTitled(myBoard, "Mon plateau"));
        boards.add(wrapTitled(enemyBoard, "Plateau adverse (cliquer pour tirer)"));

        JPanel chat = new JPanel(new BorderLayout(6, 6));
        chat.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(chatArea);
        chat.add(wrapTitled(sp, "Chat"), BorderLayout.CENTER);

        JPanel chatInputBar = new JPanel(new BorderLayout(6, 6));
        chatInputBar.add(chatInput, BorderLayout.CENTER);
        chatInputBar.add(chatSend, BorderLayout.EAST);
        chat.add(chatInputBar, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boards, chat);
        split.setResizeWeight(0.75);
        add(split, BorderLayout.CENTER);

        chatSend.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());

        myBoard.setOnCellClick((x, y) -> {
            if (!inPlacement || serverPlacementComplete) return;
            if (fleetIndex >= fleet.length) {
                status.setText("Tous les navires sont places. Cliquez « Terminer et pret».");
                return;
            }
            if (placementPending) {
                status.setText("Dernier placement en attente de confirmation...");
                return;
            }
            pendX = x; pendY = y; pendSize = fleet[fleetIndex]; pendOri = orientation;
            out.println("PLACE " + pendX + " " + pendY + " " + pendSize + " " + pendOri);
            placementPending = true;
            status.setText("Placement envoye : (" + pendX + "," + pendY + "), taille=" + pendSize + ", dir=" + pendOri);
        });

        enemyBoard.setOnCellClick((x, y) -> {
            if (!yourTurn || inPlacement) return;
            if (enemyBoard.isMarked(x, y)) { status.setText("Case deja visee."); return; }
            out.println("SHOT " + x + " " + y);
        });

        setSize(1200, 650);
        setLocationRelativeTo(null);
    }

    private void sendChat() {
        String txt = chatInput.getText().trim();
        if (txt.isEmpty() || out == null) return;
        out.println("CHAT " + txt);
        appendChat("Moi", txt);
        chatInput.setText("");
    }

    private void appendChat(String who, String msg) {
        chatArea.append(who + " : " + msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private static String orientationDesc(char o) {
        return switch (o) {
            case 'H' -> "H (haut)";
            case 'B' -> "B (bas)";
            case 'G' -> "G (gauche)";
            default -> "D (droite)";
        };
    }

    private JPanel wrapTitled(JComponent c, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    // ========= Authentification UDP =========
    private boolean doUdpIdentification(String host, int udpPort, String nick) {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(3000); // 3s de timeout
            String msg = "IDENT " + nick;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName(host),
                    udpPort
            );
            ds.send(p);

            byte[] buf = new byte[256];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            ds.receive(resp);
            String rep = new String(resp.getData(), 0, resp.getLength(), StandardCharsets.UTF_8).trim();

            if (rep.startsWith("IDENT_OK")) {
                appendChat("Système", "Identification UDP reussie pour " + nick + ".");
                return true;
            } else {
                appendChat("Système", "echec de l'identification UDP : " + rep);
                JOptionPane.showMessageDialog(this,
                        "Identification UDP refusee : " + rep,
                        "Erreur UDP",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de l'identification UDP : " + e.getMessage(),
                    "Erreur UDP",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Connexion (UDP IDENT -> TCP jeu)
    private void connectAndRun() throws IOException {
        String host = "127.0.0.1";
        int portTCP = 12345;
        int portUDP = 12346;

        String nick = JOptionPane.showInputDialog(this, "Entrez votre pseudo :", "JOUEUR");
        if (nick == null || nick.isBlank()) nick = "JOUEUR";

        Object[] opts = {"Contre un joueur", "Contre l'IA"};
        int sel = JOptionPane.showOptionDialog(this,
                "Choisissez le mode :", "Mode de jeu",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opts, opts[0]);
        chosenMode = (sel == 1) ? "AI" : "PVP";

        // 1) UDP dentification
        if (!doUdpIdentification(host, portUDP, nick)) {
            //L'authentification Ã©choue et se termine immÃ©diatement
            return;
        }

        // 2) TCP connecter + protocole de jeu
        socket = new Socket(host, portTCP);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(socket.getOutputStream(), true);

        Thread t = new Thread(this::readLoop, "reader");
        t.setDaemon(true);
        t.start();

        out.println("NAME " + nick);
        out.println("MODE " + chosenMode);
        appendChat("Système", "Mode sélectionné : " + chosenMode);
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line.trim();
                SwingUtilities.invokeLater(() -> handleServer(msg));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Déconnecté du serveur."));
        }
    }

    private void handleServer(String m) {
        System.out.println("[SERVEUR] " + m);

        // Chat
        if (m.startsWith("CHAT_FROM")) {
            String rest = m.substring("CHAT_FROM".length()).trim();
            int sp = rest.indexOf(' ');
            if (sp > 0) appendChat(rest.substring(0, sp), rest.substring(sp + 1));
            else appendChat("Adversaire", rest);
            return;
        }

        // Durée de la partie
        if (m.startsWith("GAME_TIME")) {
            String[] ps = m.split("\\s+");
            if (ps.length >= 2) {
                try {
                    int sec = Integer.parseInt(ps[1]);
                    String txt = "Duree de la partie : " + sec + " s";
                    status.setText(txt);
                    appendChat("Systeme", txt);
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        if (m.startsWith("WELCOME") || m.startsWith("HELLO")) { status.setText("Connecte. En attente d'un adversaire..."); return; }
        if (m.startsWith("WAITING")) { status.setText("En attente d'un adversaire..."); return; }
        if (m.startsWith("MATCHED")) { status.setText("Appaire : " + m.substring("MATCHED".length()).trim()); return; }

        if (m.startsWith("BOARD_SIZE")) {
            String[] ps = m.split("\\s+");
            if (ps.length >= 3) {
                boardSize = Integer.parseInt(ps[1]);
                myBoard.setBoardSize(boardSize);
                enemyBoard.setBoardSize(boardSize);
            }
            return;
        }

        if (m.startsWith("PLACE_FLEET")) {
            inPlacement = true; fleetIndex = 0; placedCount = 0;
            serverPlacementComplete = false; placementPending = false;
            status.setText("Placez votre flotte sur le plateau de gauche.");
            return;
        }

        if (m.startsWith("RESET_OK")) {
            inPlacement = true; fleetIndex = 0; placedCount = 0; serverPlacementComplete = false;
            status.setText("Placement reinitialise. Placez le premier navire.");
            return;
        }

        if (m.startsWith("PLACED")) {
            placementPending = false; placedCount++;
            if (inPlacement && fleetIndex < fleet.length && pendSize == Integer.parseInt(m.split("\\s+")[1])) {
                myBoard.placeShip(pendX, pendY, pendSize, pendOri);
                fleetIndex++;
                status.setText(fleetIndex < fleet.length ? ("Navire place. Prochain : taille " + fleet[fleetIndex]) :
                        "Tous les navires sont places. Cliquez « Terminer et pret ».");
            }
            return;
        }

        if (m.startsWith("PLACEMENT_DONE")) { serverPlacementComplete = true; status.setText("Pret cote serveur : en attente de l'adversaire..."); return; }
        if (m.startsWith("ERROR")) { placementPending = false; status.setText("Erreur serveur : " + m); return; }

        if (m.startsWith("GAME_START")) { inPlacement = false; status.setText("Partie démarree !"); return; }
        if (m.startsWith("YOUR_TURN")) { yourTurn = true; status.setText("A vous de jouer ! Cliquez sur le plateau adverse."); return; }
        if (m.startsWith("OPPONENT_TURN")) { yourTurn = false; status.setText("Tour de l'adversaire..."); return; }

        if (inPlacement) return;

        if (m.startsWith("RESULT")) {
            String[] ps = m.split("\\s+");
            if (ps.length >= 4) {
                String typ = ps[1];
                int x = Integer.parseInt(ps[2]);
                int y = Integer.parseInt(ps[3]);
                switch (typ) {
                    case "ALREADY" -> status.setText("Case déjà visée.");
                    case "MISS" -> { enemyBoard.markMiss(x, y); status.setText("Manque ("+x+","+y+")."); }
                    case "HIT"  -> { enemyBoard.markHit(x, y);  status.setText("Touche ("+x+","+y+") !"); }
                    case "SUNK" -> { enemyBoard.markHit(x, y);  status.setText("Touche et coule !"); }
                }
            }
            return;
        }

        if (m.startsWith("OPPONENT_")) {
            String[] ps = m.split("\\s+");
            if (ps.length >= 3) {
                String typ = ps[0].substring("OPPONENT_".length());
                int x = Integer.parseInt(ps[1]);
                int y = Integer.parseInt(ps[2]);
                switch (typ) {
                    case "ALREADY" -> {}
                    case "MISS" -> myBoard.markMiss(x, y);
                    case "HIT", "SUNK" -> myBoard.markHit(x, y);
                }
            }
            return;
        }

        if (m.startsWith("YOU_WIN"))  { yourTurn = false; JOptionPane.showMessageDialog(this,"Victoire !"); status.setText("Partie terminee : vous avez gagne"); return; }
        if (m.startsWith("YOU_LOSE")) { yourTurn = false; JOptionPane.showMessageDialog(this,"Defaite.");  status.setText("Partie terminee : vous avez perdu");  return; }
        if (m.startsWith("OPPONENT_DISCONNECTED")) { JOptionPane.showMessageDialog(this,"L'adversaire s'est deconnecte. Victoire par forfait."); status.setText("Partie terminee : adversaire deconnecte"); }
    }

    // ===== Plateau graphique =====
    private static class BoardPanel extends JPanel {
        private int size = 10, cell = 40, margin = 20;
        private final boolean my;
        private final Set<Point> ships = new HashSet<>();
        private final Set<Point> hits = new HashSet<>();
        private final Set<Point> misses = new HashSet<>();
        private BiConsumer<Integer,Integer> onCellClick;

        BoardPanel(boolean my) {
            this.my = my;
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int x = (e.getX() - margin) / cell;
                    int y = (e.getY() - margin) / cell;
                    if (x >= 0 && x < size && y >= 0 && y < size && onCellClick != null) onCellClick.accept(x, y);
                }
            });
        }
        void setOnCellClick(BiConsumer<Integer,Integer> cb) { this.onCellClick = cb; }
        void setBoardSize(int s) { this.size = s; repaint(); }
        void clear() { ships.clear(); hits.clear(); misses.clear(); repaint(); }
        boolean isMarked(int x, int y) { Point p = new Point(x,y); return hits.contains(p) || misses.contains(p); }
        boolean placeShip(int x, int y, int len, char dir) {
            Set<Point> tmp = new HashSet<>();
            int cx = x, cy = y;
            for (int i = 0; i < len; i++) {
                if (cx < 0 || cx >= size || cy < 0 || cy >= size) return false;
                Point p = new Point(cx, cy);
                if (ships.contains(p)) return false;
                tmp.add(p);
                switch (dir) { case 'H' -> cy--; case 'B' -> cy++; case 'G' -> cx--; default -> cx++; }
            }
            ships.addAll(tmp); repaint(); return true;
        }
        void markHit(int x, int y) { hits.add(new Point(x,y)); repaint(); }
        void markMiss(int x, int y) { misses.add(new Point(x,y)); repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = margin*2 + cell*size, h = margin*2 + cell*size;
            if (getWidth() < w || getHeight() < h) {
                cell = Math.max(20, Math.min((getWidth()-margin*2)/size, (getHeight()-margin*2)/size));
            }
            g2.setColor(Color.GRAY);
            for (int i = 0; i <= size; i++) {
                g2.drawLine(margin, margin + i*cell, margin + size*cell, margin + i*cell);
                g2.drawLine(margin + i*cell, margin, margin + i*cell, margin + size*cell);
            }
            if (my) {
                g2.setColor(new Color(0,120,215,120));
                for (Point p : ships) g2.fillRect(margin+p.x*cell+1, margin+p.y*cell+1, cell-1, cell-1);
            }
            for (Point p : misses) {
                int cx = margin + p.x*cell + cell/2, cy = margin + p.y*cell + cell/2;
                g2.setColor(new Color(30,144,255)); g2.fillOval(cx-5, cy-5, 10, 10);
            }
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(220,20,60));
            for (Point p : hits) {
                int x0 = margin + p.x*cell, y0 = margin + p.y*cell;
                g2.drawLine(x0+4, y0+4, x0+cell-4, y0+cell-4);
                g2.drawLine(x0+cell-4, y0+4, x0+4, y0+cell-4);
            }
        }
        @Override public Dimension getPreferredSize() { return new Dimension(margin*2 + cell*size, margin*2 + cell*size); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client ui = new Client();
            ui.setVisible(true);
            try {
                ui.connectAndRun();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ui, "Impossible de se connecter au serveur : " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
