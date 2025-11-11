package serveur;

import Regles.Plateau;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Gérez un tour PVP: matchmaking, formation aléatoire,
 * tir à tour de rôle et détermination de la victoire ou de la défaite.
 * Règles simplifiées : changez de main lorsque vous touchez ou manquez la cible ;
 * frapper la même case à plusieurs reprises -> ALREADY。
 */
public class SessionJeu {
    // file d'attente mondiale
    private static final Queue<ClientHandler> LISTE_ATTENTE = new LinkedList<>();

    // Données par jeu
    private final ClientHandler j1;
    private final ClientHandler j2;
    private final Plateau p1;
    private final Plateau p2;
    private volatile boolean gameOver = false;
    private volatile ClientHandler current;
    private volatile JoueurIA ia;
    private boolean modeIA = false;

    private static final int SIZE = 10;
    private static final int[] FLEET = {5, 4, 3, 3, 2};

    //Mode PVP
    public SessionJeu(ClientHandler a, ClientHandler b) {
        this.j1 = a; this.j2 = b;
        this.p1 = new Plateau(SIZE, SIZE);
        this.p2 = new Plateau(SIZE, SIZE);
        // 绑定
        j1.setSession(this);
        j2.setSession(this);
    }
    
    //Mode PVE
    public SessionJeu(ClientHandler joueur) {
        this.j1 = joueur;
        this.j2 = null;
        this.p1 = new Plateau(SIZE, SIZE);
        this.p2 = null;
        this.modeIA = true;
        joueur.setSession(this);
        startIA();
    }

    /** Rejoignez l'équipe et essayez de faire correspondre */
    public static synchronized void enqueue(ClientHandler ch) {
        LISTE_ATTENTE.add(ch);
        ch.sendMessage("WAITING for opponent...");
        if (LISTE_ATTENTE.size() >= 2) {
            ClientHandler a = LISTE_ATTENTE.poll();
            ClientHandler b = LISTE_ATTENTE.poll();
            if (a != null && b != null) {
                SessionJeu s = new SessionJeu(a, b);
                s.startPVP();
            }
        }
    }

    /** Début: formation aléatoire, tirage au sort en premier, notification aux deux parties */
    private synchronized void startPVP() {
        p1.placerFlotteAleatoire(FLEET);
        p2.placerFlotteAleatoire(FLEET);

        j1.sendMessage("MATCHED " + j2.getNomJoueur());
        j2.sendMessage("MATCHED " + j1.getNomJoueur());
        j1.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);
        j2.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);

        current = new Random().nextBoolean() ? j1 : j2;
        broadcast("GAME_START");
        promptTurn();
    }

    /** Début: formation aléatoire, j1 en premier, notification uniquement à j1 */
    private synchronized void startIA() {
    	 p1.placerFlotteAleatoire(FLEET);
    	 ia = new JoueurIA(SIZE, SIZE, FLEET);

    	 j1.sendMessage("MATCHED IA");
    	 j1.sendMessage("BOARD_SIZE " + SIZE + " " + SIZE);
    	 j1.sendMessage("GAME_START");
    	 current = j1;
    	 j1.sendMessage("YOUR_TURN");
    }

    private void broadcast(String msg) {
        j1.sendMessage(msg);
        j2.sendMessage(msg);
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

    public synchronized void handleCommand(ClientHandler from, String line) {
        if (gameOver) return;
        String cmd = line.trim();
        if (cmd.toUpperCase().startsWith("SHOT")) {
            if (from != current) {
                from.sendMessage("ERROR Not your turn");
                return;
            }
            // parse SHOT x y
            String[] parts = cmd.split("\\s+|,");
            if (parts.length < 3) {
                from.sendMessage("ERROR Usage: SHOT x y");
                return;
            }
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                processShot(from, x, y);
            } catch (NumberFormatException e) {
                from.sendMessage("ERROR Bad coordinates");
            }
        } else {
            from.sendMessage("ERROR Unknown command");
        }
    }

    private void processShot(ClientHandler shooter, int x, int y) {
    	//Mode PVE
    	if (modeIA) {
            Plateau cible = ia.getPlateau();
            String res = cible.tirer(x, y);

            j1.sendMessage("RESULT " + res + " " + x + " " + y);
            if (cible.tousLesBateauxCoules()) {
                j1.sendMessage("YOU_WIN");
                gameOver = true;
                return;
            }

            // Tour de l’IA
            jouerTourIA();
            return;
        }
    	
    	//Mode PVP
        ClientHandler defender = (shooter == j1) ? j2 : j1;
        Plateau target = (shooter == j1) ? p2 : p1;

        String res = target.tirer(x, y); // "ALREADY" | "MISS" | "HIT" | "SUNK"
        switch (res) {
            case "ALREADY":
                shooter.sendMessage("RESULT ALREADY " + x + " " + y);
                defender.sendMessage("OPPONENT_ALREADY " + x + " " + y);
                // Ne change pas de turn
                return;
            case "MISS":
                shooter.sendMessage("RESULT MISS " + x + " " + y);
                defender.sendMessage("OPPONENT_MISS " + x + " " + y);
                swapTurn();
                break;
            case "HIT":
                shooter.sendMessage("RESULT HIT " + x + " " + y);
                defender.sendMessage("OPPONENT_HIT " + x + " " + y);
                // Les hits changent également de mains (simplifié)
                swapTurn();
                break;
            case "SUNK":
                shooter.sendMessage("RESULT SUNK " + x + " " + y);
                defender.sendMessage("OPPONENT_SUNK " + x + " " + y);
                if (target.tousLesBateauxCoules()) {
                    shooter.sendMessage("YOU_WIN");
                    defender.sendMessage("YOU_LOSE");
                    gameOver = true;
                    return;
                }
                swapTurn();
                break;
        }
    }

    private void swapTurn() {
        current = (current == j1) ? j2 : j1;
        promptTurn();
    }
    
    private void jouerTourIA() {
        if (gameOver) return;
        int[] tir = ia.choisirCible();
        int x = tir[0], y = tir[1];
        String res = p1.tirer(x, y);
        j1.sendMessage("OPPONENT_" + res + " " + x + " " + y);

        if (p1.tousLesBateauxCoules()) {
            j1.sendMessage("YOU_LOSE");
            gameOver = true;
            return;
        }

        // Retour au joueur
        j1.sendMessage("YOUR_TURN");
    }

    /** Déconnexion: l'autre côté gagne et termine directement */
    public synchronized void onDisconnect(ClientHandler who) {
        if (gameOver) return;
        if (who == j1) {
            j2.sendMessage("OPPONENT_DISCONNECTED");
            j2.sendMessage("YOU_WIN");
        } else if (who == j2) {
            j1.sendMessage("OPPONENT_DISCONNECTED");
            j1.sendMessage("YOU_WIN");
        }
        gameOver = true;
    }
}