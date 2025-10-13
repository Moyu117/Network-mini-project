
public class Bateau {
	int x, y;
	int taille; //entre 2 et 5
	String OrientationGD; //seulement "G" ou "D"
	String OrientationHB; // seulement "H" ou "B"
	boolean vivant;

	public Bateau(int x, int y, int taille, String OrientationGD, String OrientationHB) {
		if((OrientationGD != "G" || OrientationGD !="D") || (OrientationHB != "H" || OrientationHB != "B")) {
			System.out.println("Construction impossible");
		}
		else {
			this.setX(x);
			this.setY(y);
			this.setTaille(taille);
			this.setOrientationGD(OrientationGD);
			this.setOrientationHB(OrientationHB);
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

	public String getOrientationGD() {
		return OrientationGD;
	}

	public void setOrientationGD(String orientationGD) {
		if(orientationGD != "G" || orientationGD != "D") {
			System.out.println("OrientationGD invalide");
		}
		else
			OrientationGD = orientationGD;
	}

	public String getOrientationHB() {
		return OrientationHB;
	}

	public void setOrientationHB(String orientationHB) {
		if(orientationHB != "H" || orientationHB != "B") {
			System.out.println("OrientationHB invalide");
		}
		else
			OrientationGD = orientationHB;
	}

	public boolean isVivant() {
		return vivant;
	}

	public void setVivant(boolean vivant) {
		this.vivant = vivant;
	}

}
