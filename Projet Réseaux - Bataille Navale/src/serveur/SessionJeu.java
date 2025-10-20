package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import Regles.Bateau;
import Regles.Plateau;

public class SessionJeu {
    public static Queue<ClientHandler> ListeAttente = new LinkedList<>();

    private ClientHandler joueur1;
    private ClientHandler joueur2;
    private JoueurIA ia;
    private BufferedReader in;
    private PrintWriter out;
    private Plateau plateau;

    public SessionJeu(ClientHandler joueur1, JoueurIA ia) {
        this.joueur1 = joueur1;
        this.ia = ia;
    }

    public SessionJeu(ClientHandler j1, ClientHandler j2) {
        this.joueur1 = j1;
        this.joueur2 = j2;
    }

    public void start() {
        
        try {
			if (ia != null) {
			    joueur1.sendMessage("Vous jouez contre l'ordinateur !");
			    // logiques du jeu vs IA ici...
			    
			    //Décider du nombre de bateaux de la partie
			    joueur1.sendMessage("Combien de bâteaux doit compter la partie ? (maximum = 5)");
			    int nbBateaux = in.read();
			    while(nbBateaux < 0 || nbBateaux > 5) {
			    	joueur1.sendMessage("Nombre de bâteaux non valide, veuillez réessayer : ");
			    	nbBateaux = in.read();
			    }
			    
			    //Placer le nombre de bateaux renseigné précédemment
			    for(int i = 0; i < nbBateaux; i++) {
			    	int x, y, t;
			    	String o;
			    	joueur1.sendMessage("Choisissez les coordonnées du " + (i + 1) + "ème bateau : " );
			    	joueur1.sendMessage("Coordonée X (entre 0 et " + plateau.getLongueur() + ": ");
			    	x = Integer.parseInt(in.readLine());
			    	joueur1.sendMessage("Coordonée Y (entre 0 et " + plateau.getLargeur() + ": ");
			    	y = Integer.parseInt(in.readLine());
			    	joueur1.sendMessage("Choisissez la taille de ce bâteau (entre 2 et 5) : ");
			    	t = in.read();
			    	joueur1.sendMessage("Orientation du bâteau (uniquement 'H', 'B', 'G', 'D') :  ");
			    	o = in.readLine();
			    	
			    	Bateau b = new Bateau(x, y, t, o);
			    	
			    	while(!plateau.placerBateau(b)) {
			    		joueur1.sendMessage("Construction du bâteau " + (i + 1) + " impossible, veuillez réessayer");
			    		joueur1.sendMessage("Coordonée X (entre 0 et " + plateau.getLongueur() + ": ");
				    	x = Integer.parseInt(in.readLine());
				    	joueur1.sendMessage("Coordonée Y (entre 0 et " + plateau.getLargeur() + ": ");
				    	y = Integer.parseInt(in.readLine());
				    	joueur1.sendMessage("Choisissez la taille de ce bâteau (entre 2 et 5) : ");
				    	t = in.read();
				    	joueur1.sendMessage("Orientation du bâteau (uniquement 'H', 'B', 'G', 'D') :  ");
				    	o = in.readLine();
				    	
				    	b = new Bateau(x, y, t, o);
			    	}
			    	
			    	joueur1.sendMessage("Bâteau " + (i + 1) + "placé !" );
			    }
			    joueur1.sendMessage("Tous vos bâteaux ont été placés");
			    
			    //placer les bateaux de l'ordinateur
			    for(int i = 0; i < nbBateaux; i++) {
			    	int x, y, t;
			    	char c;
			    	String o;
			    	Random r = new Random();
			    	x = r.nextInt(plateau.getLongueur());
			    	y = r.nextInt(plateau.getLargeur());
			    	t = r.nextInt(4) + 2;
			    	
			    	String opossibles = "HBGD";
			    	c = opossibles.charAt(r.nextInt(opossibles.length()));
			    	o = String.valueOf(c);
			    	
			    	Bateau b = new Bateau(x, y, t, o);
			    	
			    	//On teste si le bâteau généré par l'ordinateur peut être mis en place
			    	while(!plateau.placerBateau(b)) {
			    		//Si on arrive ici, la construction du bâteau généré par l'ordinateur est impossible
			    		//On le fait recommencer jusqu'à ce que ce soit bon
			    		x = r.nextInt(plateau.getLongueur());
				    	y = r.nextInt(plateau.getLargeur());
				    	t = r.nextInt(4) + 2;
				    	c = opossibles.charAt(r.nextInt(opossibles.length()));
				    	o = String.valueOf(c);
				    	b = new Bateau(x, y, t, o);
			    	}
			    }
			    
			    joueur1.sendMessage("L'ordinateur a placé ses bâteaux, la partie va pouvoir commencer !");
			    
			    /*
			     * Déroulement entier de la partie entre le joueur1 et l'ordinateur
			     */
			    
			    
			} else {
				joueur1.sendMessage("Partie commencée contre " + joueur2.getNomJoueur());
				joueur2.sendMessage("Partie commencée contre " + joueur1.getNomJoueur());
			    // logiques du jeu joueur vs joueur ici...
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
