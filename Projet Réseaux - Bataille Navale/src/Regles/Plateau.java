package Regles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plateau de taille longueur x largeur.
 * Convention: (x,y) où x ∈ [0..largeur-1], y ∈ [0..longueur-1]
 * Grille stockée en [y][x].
 */
public class Plateau {
    private final int longueur; // rows
    private final int largeur;  // cols
    private final char[][] grille;   // '.' vide, 'B' bateau, 'X' touché, 'O' à l'eau
    private final List<Bateau> bateaux = new ArrayList<>();

    public Plateau(int longueur, int largeur) {
        if (longueur <= 0 || largeur <= 0)
            throw new IllegalArgumentException("Dimensions invalides");
        this.longueur = longueur;
        this.largeur = largeur;
        this.grille = new char[longueur][largeur];
        initialiserPlateau();
    }

    private void initialiserPlateau() {
        for (int y = 0; y < longueur; y++)
            for (int x = 0; x < largeur; x++)
                grille[y][x] = '.';
    }

    public int getLongueur() { return longueur; }
    public int getLargeur() { return largeur; }
    public char[][] getGrille() { return grille; }
    public List<Bateau> getBateaux() { return bateaux; }

    public boolean dansPlateau(int x, int y) {
        return x >= 0 && x < largeur && y >= 0 && y < longueur;
    }

    private boolean peutPlacer(Bateau b) {
        for (int[] p : b.getPositions()) {
            int x = p[0], y = p[1];
            if (!dansPlateau(x, y)) return false;
            if (grille[y][x] == 'B') return false;
        }
        return true;
    }

    public boolean placerBateau(Bateau b) {
        if (!peutPlacer(b)) return false;
        for (int[] p : b.getPositions()) {
            grille[p[1]][p[0]] = 'B';
        }
        bateaux.add(b);
        return true;
    }

    /** Résultat de tir : "ALREADY" | "MISS" | "HIT" | "SUNK" */
    public String tirer(int x, int y) {
        if (!dansPlateau(x, y)) return "MISS";
        if (grille[y][x] == 'X' || grille[y][x] == 'O') return "ALREADY";

        if (grille[y][x] == 'B') {
            // Trouver le bateau touché
            for (Bateau b : bateaux) {
                if (b.indexOf(x, y) >= 0) {
                    b.hit(x, y);
                    grille[y][x] = 'X';
                    return b.estCoule() ? "SUNK" : "HIT";
                }
            }
            // fallback (devrait pas arriver)
            grille[y][x] = 'X';
            return "HIT";
        } else {
            grille[y][x] = 'O';
            return "MISS";
        }
    }

    public boolean tousLesBateauxCoules() {
        for (Bateau b : bateaux) if (!b.estCoule()) return false;
        return true;
    }

    /** Placement aléatoire d'une flotte, par ex. {5,4,3,3,2} */
    public void placerFlotteAleatoire(int[] tailles) {
        Random r = new Random();
        for (int t : tailles) {
            boolean ok = false;
            for (int tries = 0; tries < 1000 && !ok; tries++) {
                int x = r.nextInt(largeur);
                int y = r.nextInt(longueur);
                char[] os = {'H','B','G','D'};
                char o = os[r.nextInt(os.length)];
                Bateau b = new Bateau(x, y, t, String.valueOf(o));
                if (placerBateau(b)) ok = true;
            }
            if (!ok) throw new IllegalStateException("Impossible de placer la flotte aléatoire");
        }
    }

    public void afficherPlateau() {
        System.out.print("   ");
        for (int x = 0; x < largeur; x++) System.out.print(x + " ");
        System.out.println();
        for (int y = 0; y < longueur; y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < largeur; x++) System.out.print(grille[y][x] + " ");
            System.out.println();
        }
    }
}