package serveur;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class Client extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final BoardPanel myBoard = new BoardPanel(true);
    private final BoardPanel enemyBoard = new BoardPanel(false);
    private final JLabel status = new JLabel("Bienvenue");
    private final JButton rotateBtn = new JButton("Orientation: D (右)");
    private final JButton resetBtn = new JButton("Réinitialiser l'emplacement");
    private final JButton readyBtn = new JButton("complet et prêt");

    private int boardSize = 10;
    private boolean inPlacement = true;
    private boolean yourTurn = false;
    private char orientation = 'D';
    private int[] fleet = {5,4,3,3,2};
    private int fleetIndex = 0;

    private boolean placementPending = false;
    private int pendX, pendY, pendSize;
    private char pendOri;

    public Client() {
        super("Bataille Navale - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

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
            rotateBtn.setText("Orientation: " + orientationDesc(orientation));
        });

        resetBtn.addActionListener(e -> {
            if (!inPlacement) return;
            myBoard.clear();
            enemyBoard.clear();
            fleetIndex = 0;
            placementPending = false;
            status.setText("Dégagé, repositionné avec taille " + fleet[fleetIndex] + " navire de guerre");
            if (out != null) out.println("RESET");   // 🔧 通知服务器同步重置
        });

        readyBtn.addActionListener(e -> {
            if (inPlacement && fleetIndex >= fleet.length) out.println("READY");
            else status.setText("Il y a aussi des cuirassés non placés.");
        });

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        center.add(wrapTitled(myBoard, "Mon plateau"));
        center.add(wrapTitled(enemyBoard, "Plateau adverse (cliquer pour tirer)"));
        add(center, BorderLayout.CENTER);

        myBoard.setOnCellClick((x, y) -> {
            if (!inPlacement) return;
            if (fleetIndex >= fleet.length) { status.setText("La flotte a été libérée, veuillez cliquer sur «Terminer et préparer»."); return; }
            if (placementPending) { status.setText("Le dernier placement est en cours de confirmation…"); return; }
            pendX = x; pendY = y; pendSize = fleet[fleetIndex]; pendOri = orientation;
            out.println("PLACE " + pendX + " " + pendY + " " + pendSize + " " + pendOri);
            placementPending = true;
            status.setText("Envoyer l'emplacement：" + pendX + "," + pendY + " size=" + pendSize + " dir=" + pendOri);
        });

        enemyBoard.setOnCellClick((x, y) -> {
            if (!yourTurn || inPlacement) return;
            if (enemyBoard.isMarked(x, y)) { status.setText("Tu as déjà attaque ici。"); return; }
            out.println("SHOT " + x + " " + y);
        });

        setSize(980, 600);
        setLocationRelativeTo(null);
    }

    private static String orientationDesc(char o) {
        return switch (o) {
            case 'H' -> "H (上)";
            case 'B' -> "B (下)";
            case 'G' -> "G (左)";
            default -> "D (右)";
        };
    }

    private JPanel wrapTitled(JComponent c, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void connectAndRun() throws IOException {
        String host = "2.tcp.cpolar.top";
        int port = 15442;
        String nick = JOptionPane.showInputDialog(this, "Entrez votre pseudo:", "PLAYER");
        if (nick == null || nick.isBlank()) nick = "PLAYER";

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(socket.getOutputStream(), true);

        Thread t = new Thread(this::readLoop, "reader");
        t.setDaemon(true);
        t.start();

        out.println("NAME " + nick);
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
        System.out.println("[SERVER] " + m);

        if (m.startsWith("WELCOME") || m.startsWith("HELLO")) { status.setText("Connecté au serveur, en attente de match…"); return; }
        if (m.startsWith("WAITING")) { status.setText("Attendez que les adversaires se joignent…"); return; }
        if (m.startsWith("MATCHED")) { status.setText("Match réussi：" + m.substring("MATCHED".length()).trim()); return; }

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
            inPlacement = true;
            fleetIndex = 0;
            placementPending = false;
            status.setText("Veuillez placer les flottes sur l'échiquier de gauche dans l'ordre。");
            return;
        }

        if (m.startsWith("RESET_OK")) {
            // 服务器已同步重置
            inPlacement = true;
            status.setText("Réinitialisez, veuillez repositionner le premier cuirassé");
            return;
        }

        if (m.startsWith("PLACED")) {
            placementPending = false;
            if (inPlacement && fleetIndex < fleet.length &&
                    pendSize == Integer.parseInt(m.split("\\s+")[1])) {
                myBoard.placeShip(pendX, pendY, pendSize, pendOri);
                fleetIndex++;
                status.setText(fleetIndex < fleet.length ?
                        ("Placé, la taille suivante est " + fleet[fleetIndex]) :
                        "La flotte a été placée, cliquez sur «Terminer et préparer».");
            }
            return;
        }

        if (m.startsWith("PLACEMENT_DONE")) { status.setText("Prêt et attendant l'adversaire..."); return; }

        if (m.startsWith("ERROR")) {
            placementPending = false;
            status.setText("Erreur de serveur：" + m);
            return;
        }

        if (m.startsWith("GAME_START")) { inPlacement = false; status.setText("Le jeu commence！"); return; }
        if (m.startsWith("YOUR_TURN")) { yourTurn = true; status.setText("a vous jouer！"); return; }
        if (m.startsWith("OPPONENT_TURN")) { yourTurn = false; status.setText("le tour de l'adversaire…"); return; }

        // ⛔️ 摆放阶段忽略一切命中/脱靶渲染，避免“未开战先出现蓝点”的假象
        if (inPlacement) return;

        if (m.startsWith("RESULT")) {
            String[] ps = m.split("\\s+");
            if (ps.length >= 4) {
                String typ = ps[1];
                int x = Integer.parseInt(ps[2]);
                int y = Integer.parseInt(ps[3]);
                switch (typ) {
                    case "ALREADY" -> status.setText("Cet endroit a été touché。");
                    case "MISS" -> { enemyBoard.markMiss(x, y); status.setText("manquer ("+x+","+y+")"); }
                    case "HIT"  -> { enemyBoard.markHit(x, y);  status.setText("frapper ("+x+","+y+")！"); }
                    case "SUNK" -> { enemyBoard.markHit(x, y);  status.setText("frapper et couler！"); }
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

        if (m.startsWith("YOU_WIN")) { yourTurn = false; JOptionPane.showMessageDialog(this,"你赢了！"); status.setText("游戏结束：你赢了"); return; }
        if (m.startsWith("YOU_LOSE")) { yourTurn = false; JOptionPane.showMessageDialog(this,"你输了。"); status.setText("游戏结束：你输了"); return; }
        if (m.startsWith("OPPONENT_DISCONNECTED")) { JOptionPane.showMessageDialog(this,"对手断开，你胜利。"); status.setText("游戏结束：对手断线"); }
    }

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
                JOptionPane.showMessageDialog(ui, "ne peut pas atteindre le serveur：" + e.getMessage());
                System.exit(1);
            }
        });
    }
}
