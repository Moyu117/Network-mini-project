package serveur;

import java.util.Random;

import Regles.Plateau;

public class JoueurIA {
	private final Plateau plateau;
    private final Random rand = new Random();
    private final int width, height;

    public JoueurIA(int width, int height, int[] flotte) {
        this.width = width;
        this.height = height;
        plateau = new Plateau(width, height);
        plateau.placerFlotteAleatoire(flotte);
    }

    public Plateau getPlateau() {
        return plateau;
    }

    /** Genere un tir aleatoire valide */
    public int[] choisirCible() {
        int x = rand.nextInt(width);
        int y = rand.nextInt(height);
        return new int[]{x, y};
    }
}
