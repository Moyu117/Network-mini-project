package Regles;
import java.util.ArrayList;

public class Bateau {
	int x, y;
	int taille; //entre 2 et 5
	String orientation; //H = haut, B = bas, G = gauche, D = droite
	boolean vivant;
	private ArrayList<int[]> positions = new ArrayList<>();

	public Bateau(int x, int y, int taille, String orientation) {
		if (!orientation.equals("H") && !orientation.equals("B") &&
	            !orientation.equals("G") && !orientation.equals("D")) {
	            throw new IllegalArgumentException("Orientation invalide : doit être H, B, G ou D");
	        }
		else if(taille < 2 || taille > 5) {
			throw new IllegalArgumentException("Taille invalide : doit être entre 2 et 5");
		}
		else {
			this.setX(x);
			this.setY(y);
			this.setTaille(taille);
			this.setOrientation(orientation);
			this.vivant = true;
		}
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getTaille() {
		return taille;
	}

	public void setTaille(int taille) {
		this.taille = taille;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		if (!orientation.equals("H") && !orientation.equals("B") &&
				!orientation.equals("G") && !orientation.equals("D")) {
				throw new IllegalArgumentException("Orientation invalide : doit être H, B, G ou D");
		 	}
		else
			this.orientation = orientation;
	}

	public boolean isVivant() {
		return vivant;
	}

	public void setVivant(boolean vivant) {
		this.vivant = vivant;
	}
	
	public ArrayList<int[]> getPositions() {
		return positions;
	}

	public void setPositions(ArrayList<int[]> positions) {
		this.positions = positions;
	}

	@Override
    public String toString() {
        return "Bateau{ x =" + this.x + ", y =" + this.y + ", taille =" + this.taille + ", orientation = " + this.orientation + ", vivant = + " + this.vivant + " }";
    }

}
