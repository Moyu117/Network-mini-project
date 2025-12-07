package serveur;

import Regles.Bateau;
import Regles.Plateau;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Session VS IA
 * RÃ¨gle complÃ¨te : si le tir touche ou coule, le tireur rejoue.
 * ChronomÃ¨tre : du GAME_START jusquâ€™Ã  la fin de la partie.
 */
public class SessionJeuAI {
    private final ClientHandler human;
    private final Plateau humanBoard = new Plateau(10,10);
    private final Plateau aiBoard    = new Plateau(10,10);

    private static final int SIZE = 10;
    private static final int[] FLEET = {5,4,3,3,2};

    private volatile boolean gameOver = false;
    private volatile boolean placementDoneHuman = false;
    private volatile boolean gameStarted = false;
    private volatile boolean humanTurn;

    private final Random r = new Random();
    private final JoueurIA ia = new JoueurIA();
    private final Set<String> aiShots = new HashSet<>();

    // Chronomètre
    private long startTimeMillis = -1;
    private long endTimeMillis   = -1;

    private SessionJeuAI(ClientHandler h) {
        this.human = h;
        h.setSessionAI(this);
    }

    public static void startFor(ClientHandler human) {
        SessionJeuAI s = new SessionJeuAI(human);
        s.start();
    }

    private synchronized void start() {
        human.sendMessage("MATCHED IA");
        human.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);

        aiBoard.placerFlotteAleatoire(FLEET);
        human.sendMessage("PLACE_FLEET 5,4,3,3,2");
    }

    public synchronized void handleCommand(ClientHandler from, String line) {
        if (gameOver) return;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;

        String[] ps = trimmed.split("\\s+");
        String cmd = ps[0].toUpperCase();

        // Chat vers IAï¼šå¿½ç•¥æˆ–ä»¥å�Žæ‰©å±•
        if (cmd.equals("CHAT")) return;

        if (!gameStarted) {
            // Phase de placement
            if (cmd.equals("PLACE")) {
                if (ps.length < 5) { human.sendMessage("ERROR Usage: PLACE x y size dir"); return; }
                try {
                    int x = Integer.parseInt(ps[1]);
                    int y = Integer.parseInt(ps[2]);
                    int size = Integer.parseInt(ps[3]);
                    char dir = Character.toUpperCase(ps[4].charAt(0));
                    if ("HBGD".indexOf(dir) < 0) { human.sendMessage("ERROR dir must be H/B/G/D"); return; }

                    Bateau b = new Bateau(x,y,size,String.valueOf(dir));
                    if (!humanBoard.placerBateau(b)) {
                        human.sendMessage("ERROR Cannot place ship (collision or out of bounds)");
                        return;
                    }
                    human.sendMessage("PLACED " + size);
                } catch (Exception e) {
                    human.sendMessage("ERROR Bad PLACE arguments");
                }
                return;
            }
            if (cmd.equals("RESET")) {
                for (int y = 0; y < 10; y++)
                    for (int x = 0; x < 10; x++)
                        humanBoard.getGrille()[y][x] = '.';
                humanBoard.getBateaux().clear();
                human.sendMessage("RESET_OK");
                human.sendMessage("PLACE_FLEET 5,4,3,3,2");
                return;
            }
            if (cmd.equals("READY")) {
                placementDoneHuman = true;
                human.sendMessage("PLACEMENT_DONE");
                startBattleIfReady();
                return;
            }
            human.sendMessage("ERROR In placement phase. Use: PLACE/RESET/READY");
            return;
        }

        // Phase de jeu
        if (cmd.equals("SHOT")) {
            if (!humanTurn) { human.sendMessage("ERROR Not your turn"); return; }
            if (ps.length < 3) { human.sendMessage("ERROR Usage: SHOT x y"); return; }
            try {
                int x = Integer.parseInt(ps[1]);
                int y = Integer.parseInt(ps[2]);
                processHumanShot(x, y);
            } catch (NumberFormatException e) {
                human.sendMessage("ERROR Bad coordinates");
            }
            return;
        }

        human.sendMessage("ERROR Unknown command");
    }

    private void startBattleIfReady() {
        if (placementDoneHuman) {
            gameStarted = true;
            startTimeMillis = System.currentTimeMillis(); // â�± å¼€å§‹è®¡æ—¶
            humanTurn = r.nextBoolean();
            human.sendMessage("GAME_START");
            if (humanTurn) {
                human.sendMessage("YOUR_TURN");
            } else {
                human.sendMessage("OPPONENT_TURN");
                aiTakeTurn();
            }
        }
    }

    private void processHumanShot(int x, int y) {
        String res = aiBoard.tirer(x, y);
        switch (res) {
            case "ALREADY":
                human.sendMessage("RESULT ALREADY " + x + " " + y);
                break;
            case "MISS":
                human.sendMessage("RESULT MISS " + x + " " + y);
                humanTurn = false;
                aiTakeTurn();
                break;
            case "HIT":
                human.sendMessage("RESULT HIT " + x + " " + y);
                // å‘½ä¸­ï¼šç»§ç»­
                humanTurn = true;
                human.sendMessage("YOUR_TURN");
                break;
            case "SUNK":
                human.sendMessage("RESULT SUNK " + x + " " + y);
                if (aiBoard.tousLesBateauxCoules()) {
                    finishGame(true);
                    return;
                }
                humanTurn = true;
                human.sendMessage("YOUR_TURN");
                break;
        }
    }

    private void aiTakeTurn() {
        if (gameOver) return;

        boolean aiContinues = true;
        while (aiContinues && !gameOver) {
            int x = 0, y = 0;
            int tries = 0;
            do {
                String s = ia.Joueur();
                String[] arr = s.split(",");
                if (arr.length == 2) {
                    try {
                        x = Integer.parseInt(arr[0].trim());
                        y = Integer.parseInt(arr[1].trim());
                    } catch (Exception ignored) {}
                }
                tries++;
                if (tries > 200) {
                    x = new Random().nextInt(10);
                    y = new Random().nextInt(10);
                }
            } while (!aiShots.add(x + "," + y));

            String res = humanBoard.tirer(x, y);
            switch (res) {
                case "ALREADY":
                    aiContinues = true; // ç�†è®ºä¸�ä¼šå�‘ç”Ÿ
                    break;
                case "MISS":
                    human.sendMessage("OPPONENT_MISS " + x + " " + y);
                    aiContinues = false;
                    humanTurn = true;
                    human.sendMessage("YOUR_TURN");
                    break;
                case "HIT":
                    human.sendMessage("OPPONENT_HIT " + x + " " + y);
                    aiContinues = true;   // å‘½ä¸­è¿žå°„
                    humanTurn = false;
                    break;
                case "SUNK":
                    human.sendMessage("OPPONENT_SUNK " + x + " " + y);
                    if (humanBoard.tousLesBateauxCoules()) {
                        finishGame(false);
                        return;
                    }
                    aiContinues = true;
                    humanTurn = false;
                    break;
            }
        }
    }

    // â�± heure de simultanÃ©itÃ© de fin de partie
    private void finishGame(boolean humanWins) {
        if (gameOver) return;
        gameOver = true;

        if (humanWins) {
            human.sendMessage("YOU_WIN");
        } else {
            human.sendMessage("YOU_LOSE");
        }

        if (startTimeMillis > 0) {
            endTimeMillis = System.currentTimeMillis();
            long seconds = (endTimeMillis - startTimeMillis) / 1000;
            human.sendMessage("GAME_TIME " + seconds);
        }
    }

    public synchronized void onDisconnect(ClientHandler who) {
        if (gameOver) return;
        // La dÃ©connexion est considÃ©rÃ©e comme un Ã©chec
        finishGame(false);
    }
}
