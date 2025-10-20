package serveur;

import java.util.LinkedList;
import java.util.Queue;

public class SessionJeu {
    public static Queue<ClientHandler> ListeAttente = new LinkedList<>();

    private ClientHandler joueur1;
    private ClientHandler joueur2;
    private JoueurIA ia;

    public SessionJeu(ClientHandler joueur1, JoueurIA ia) {
        this.joueur1 = joueur1;
        this.ia = ia;
    }

    public SessionJeu(ClientHandler j1, ClientHandler j2) {
        this.joueur1 = j1;
        this.joueur2 = j2;
    }

    public void start() {
        if (ia != null) {
            joueur1.sendMessage("Vous jouez contre l'ordinateur !");
            // logiques du jeu vs AI ici...
        } else {
        	joueur1.sendMessage("Partie commencée contre " + joueur2.getNomJoueur());
        	joueur2.sendMessage("Partie commencée contre " + joueur1.getNomJoueur());
            // logiques du jeu joueur vs joueur ici...
        }
    }
}
