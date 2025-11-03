package Regles;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un bateau : point de départ (x,y), taille [2..5], orientation H/B/G/D
 * H=haut, B=bas, G=gauche, D=droite
 */
public class Bateau {
    private int x, y;
    private int taille;               // [2..5]
    private char orientation;         // 'H','B','G','D'
    private boolean[] touches;        // taille cases
    private final List<int[]> positions = new ArrayList<>();

    public Bateau(int x, int y, int taille, String orientation) {
        if (orientation == null || orientation.isEmpty())
            throw new IllegalArgumentException("Orientation requise");
        char o = Character.toUpperCase(orientation.charAt(0));
        if (o != 'H' && o != 'B' && o != 'G' && o != 'D')
            throw new IllegalArgumentException("Orientation invalide : H/B/G/D");
        if (taille < 2 || taille > 5)
            throw new IllegalArgumentException("Taille invalide (2..5)");
        this.x = x;
        this.y = y;
        this.taille = taille;
        this.orientation = o;
        this.touches = new boolean[taille];
        recomputePositions();
    }

    private void recomputePositions() {
        positions.clear();
        int cx = x, cy = y;
        for (int i = 0; i < taille; i++) {
            positions.add(new int[]{cx, cy});
            switch (orientation) {
                case 'H': cy -= 1; break;
                case 'B': cy += 1; break;
                case 'G': cx -= 1; break;
                case 'D': cx += 1; break;
            }
        }
    }

    /** Retourne l'index de la case touchée, -1 sinon */
    public int indexOf(int sx, int sy) {
        for (int i = 0; i < positions.size(); i++) {
            int[] p = positions.get(i);
            if (p[0] == sx && p[1] == sy) return i;
        }
        return -1;
    }

    /** Enregistre un tir sur (sx,sy). Retourne true si c'est un coup au but. */
    public boolean hit(int sx, int sy) {
        int idx = indexOf(sx, sy);
        if (idx >= 0) {
            touches[idx] = true;
            return true;
        }
        return false;
    }

    public boolean estCoule() {
        for (boolean t : touches) if (!t) return false;
        return true;
    }

    public List<int[]> getPositions() { return positions; }
    public int getTaille() { return taille; }
    public char getOrientation() { return orientation; }
    public int getX() { return x; }
    public int getY() { return y; }

    public void setX(int x) { this.x = x; recomputePositions(); }
    public void setY(int y) { this.y = y; recomputePositions(); }
    public void setOrientation(char o) {
        this.orientation = Character.toUpperCase(o);
        recomputePositions();
    }

    @Override
    public String toString() {
        return "Bateau{x=" + x + ", y=" + y + ", taille=" + taille + ", o=" + orientation + "}";
    }
}
