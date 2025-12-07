package serveur;

import Regles.Bateau;
import Regles.Plateau;

import java.util.*;

public class SessionJeu {
    private static final Queue<ClientHandler> LISTE_ATTENTE = new LinkedList<>();

    private enum Phase { PLACEMENT, RUNNING, OVER }

    private final ClientHandler j1, j2;
    private final Plateau p1 = new Plateau(10, 10);
    private final Plateau p2 = new Plateau(10, 10);

    private volatile ClientHandler current;
    private volatile Phase phase = Phase.PLACEMENT;
    private volatile boolean gameOver = false;

    private static final int SIZE = 10;
    private static final int[] FLEET = {5, 4, 3, 3, 2};

    private final Map<ClientHandler, Map<Integer, Integer>> remaining = new HashMap<>();
    private final Set<ClientHandler> readySet = new HashSet<>();

    // Chronomètre
    private long startTimeMillis = -1;
    private long endTimeMillis   = -1;

    public SessionJeu(ClientHandler a, ClientHandler b) {
        this.j1 = a; this.j2 = b;
        j1.setSession(this);
        j2.setSession(this);
        remaining.put(j1, fleetBag());
        remaining.put(j2, fleetBag());
    }

    private Map<Integer, Integer> fleetBag() {
        Map<Integer, Integer> bag = new HashMap<>();
        for (int s : FLEET) bag.put(s, bag.getOrDefault(s, 0) + 1);
        return bag;
    }

    public static synchronized void enqueue(ClientHandler ch) {
        LISTE_ATTENTE.add(ch);
        ch.sendMessage("WAITING for opponent...");
        if (LISTE_ATTENTE.size() >= 2) {
            ClientHandler a = LISTE_ATTENTE.poll();
            ClientHandler b = LISTE_ATTENTE.poll();
            if (a != null && b != null) new SessionJeu(a, b).start();
        }
    }

    private synchronized void start() {
        j1.sendMessage("MATCHED " + j2.getNomJoueur());
        j2.sendMessage("MATCHED " + j1.getNomJoueur());
        j1.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);
        j2.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);
        j1.sendMessage("PLACE_FLEET 5,4,3,3,2");
        j2.sendMessage("PLACE_FLEET 5,4,3,3,2");
        phase = Phase.PLACEMENT;
    }

    private void broadcast(String msg) { j1.sendMessage(msg); j2.sendMessage(msg); }
    private Plateau boardOf(ClientHandler ch) { return (ch == j1) ? p1 : p2; }
    private Plateau enemyBoardOf(ClientHandler ch) { return (ch == j1) ? p2 : p1; }
    private ClientHandler opponentOf(ClientHandler ch) { return (ch == j1) ? j2 : j1; }

    public synchronized void handleCommand(ClientHandler from, String line) {
        if (gameOver) return;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;

        // Chat
        if (trimmed.toUpperCase().startsWith("CHAT")) {
            String text = trimmed.length() > 4 ? trimmed.substring(5) : "";
            opponentOf(from).sendMessage("CHAT_FROM " + from.getNomJoueur() + " " + text);
            return;
        }

        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toUpperCase();

        // Phase de placement
        if (phase == Phase.PLACEMENT) {
            if ("PLACE".equals(cmd)) { handlePlace(from, parts); return; }
            if ("RESET".equals(cmd)) { handleReset(from); return; }
            if ("READY".equals(cmd)) {
                readySet.add(from);
                from.sendMessage("PLACEMENT_DONE");
                tryStartBattleIfBothReady();
                return;
            }
            from.sendMessage("ERROR In placement phase. Use: PLACE x y size dir");
            return;
        }

        // Phase de jeu
        if (phase == Phase.RUNNING) {
            if ("SHOT".equals(cmd)) {
                if (from != current) { from.sendMessage("ERROR Not your turn"); return; }
                if (parts.length < 3) { from.sendMessage("ERROR Usage: SHOT x y"); return; }
                try {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    processShot(from, x, y);
                } catch (NumberFormatException e) {
                    from.sendMessage("ERROR Bad coordinates");
                }
                return;
            }
        }

        from.sendMessage("ERROR Unknown command");
    }

    private void handlePlace(ClientHandler from, String[] parts) {
        if (parts.length < 5) { from.sendMessage("ERROR Usage: PLACE x y size dir"); return; }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int size = Integer.parseInt(parts[3]);
            char dir = Character.toUpperCase(parts[4].charAt(0));
            if ("HBGD".indexOf(dir) < 0) { from.sendMessage("ERROR dir must be H/B/G/D"); return; }

            Map<Integer, Integer> bag = remaining.get(from);
            int left = bag.getOrDefault(size, 0);
            if (left <= 0) { from.sendMessage("ERROR No remaining ship of size " + size); return; }

            Bateau b = new Bateau(x, y, size, String.valueOf(dir));
            if (!boardOf(from).placerBateau(b)) {
                from.sendMessage("ERROR Cannot place ship (collision or out of bounds)");
                return;
            }

            bag.put(size, left - 1);
            from.sendMessage("PLACED " + size);

            if (bag.values().stream().mapToInt(i -> i).sum() == 0) {
                readySet.add(from);
                from.sendMessage("PLACEMENT_DONE");
                tryStartBattleIfBothReady();
            }
        } catch (Exception e) {
            from.sendMessage("ERROR Bad PLACE arguments");
        }
    }

    private void handleReset(ClientHandler from) {
        if (from == j1) {
            for (int y = 0; y < 10; y++)
                for (int x = 0; x < 10; x++)
                    p1.getGrille()[y][x] = '.';
            p1.getBateaux().clear();
        } else {
            for (int y = 0; y < 10; y++)
                for (int x = 0; x < 10; x++)
                    p2.getGrille()[y][x] = '.';
            p2.getBateaux().clear();
        }
        remaining.put(from, fleetBag());
        readySet.remove(from);
        from.sendMessage("RESET_OK");
        from.sendMessage("PLACE_FLEET 5,4,3,3,2");
    }

    private void tryStartBattleIfBothReady() {
        if (readySet.contains(j1) && readySet.contains(j2) && phase == Phase.PLACEMENT) {
            phase = Phase.RUNNING;
            current = new Random().nextBoolean() ? j1 : j2;
            startTimeMillis = System.currentTimeMillis(); // â�± å¼€å§‹è®¡æ—¶
            broadcast("GAME_START");
            promptTurn();
        }
    }

    private void promptTurn() {
        if (current == j1) {
            j1.sendMessage("YOUR_TURN");
            j2.sendMessage("OPPONENT_TURN");
        } else {
            j2.sendMessage("YOUR_TURN");
            j1.sendMessage("OPPONENT_TURN");
        }
    }

    /**
     * å‘½ä¸­ç»§ç»­æ”»å‡»ï¼šå�ªæœ‰ MISS æ‰�æ�¢æ‰‹ã€‚
     */
    private void processShot(ClientHandler shooter, int x, int y) {
        ClientHandler defender = opponentOf(shooter);
        Plateau target = enemyBoardOf(shooter);

        String res = target.tirer(x, y);

        switch (res) {
            case "ALREADY":
                shooter.sendMessage("RESULT ALREADY " + x + " " + y);
                defender.sendMessage("OPPONENT_ALREADY " + x + " " + y);
                // å›žå�ˆä¸�å�˜
                return;

            case "MISS":
                shooter.sendMessage("RESULT MISS " + x + " " + y);
                defender.sendMessage("OPPONENT_MISS " + x + " " + y);
                current = defender; // æ�¢æ‰‹
                promptTurn();
                break;

            case "HIT":
                shooter.sendMessage("RESULT HIT " + x + " " + y);
                defender.sendMessage("OPPONENT_HIT " + x + " " + y);
                // å‘½ä¸­ï¼šä¸�æ�¢ currentï¼Œå†�å�‘ä¸€æ¬¡å›žå�ˆæ��ç¤ºè¡¨ç¤ºå�¯ä»¥ç»§ç»­
                promptTurn();
                break;

            case "SUNK":
                shooter.sendMessage("RESULT SUNK " + x + " " + y);
                defender.sendMessage("OPPONENT_SUNK " + x + " " + y);
                if (target.tousLesBateauxCoules()) {
                    finishGame(shooter, defender);
                    return;
                }
                // å‡»æ²‰ä½†æœªç»“æ�Ÿï¼šå‘½ä¸­æ–¹ç»§ç»­
                promptTurn();
                break;
        }
    }

    // â�± ç»“æ�Ÿæ¸¸æˆ�å¹¶å¹¿æ’­æ—¶é—´
    private void finishGame(ClientHandler winner, ClientHandler loser) {
        if (gameOver) return;
        gameOver = true;
        phase = Phase.OVER;

        winner.sendMessage("YOU_WIN");
        loser.sendMessage("YOU_LOSE");

        if (startTimeMillis > 0) {
            endTimeMillis = System.currentTimeMillis();
            long seconds = (endTimeMillis - startTimeMillis) / 1000;
            winner.sendMessage("GAME_TIME " + seconds);
            loser.sendMessage("GAME_TIME " + seconds);
        }
    }

    public synchronized void onDisconnect(ClientHandler who) {
        if (gameOver) return;
        ClientHandler opp = opponentOf(who);
        opp.sendMessage("OPPONENT_DISCONNECTED");
        // æ–­çº¿ä¹Ÿè§†ä¸ºä¸€æ–¹èƒœåˆ©ï¼Œè®¡æ—¶åˆ°æ­¤ä¸ºæ­¢
        finishGame(opp, who);
    }
}
