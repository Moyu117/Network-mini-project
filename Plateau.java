import java.util.List;
import java.util.ArrayList;

public class Plateau {
	
	Object[][] Case;
	int largeur;
	int longueur;
	List<Bateau>[] bateaux;
	
	public Plateau(int longueur, int largeur) {
		this.Case = new Object[longueur][largeur];
		this.largeur = largeur;
		this.longueur = longueur;
		this.initialiserPlateau(longueur, largeur);
	}

	public int getLargeur() {
		return largeur;
	}

	public void setLargeur(int largeur) {
		this.largeur = largeur;
	}

	public int getLongueur() {
		return longueur;
	}

	public void setLongueur(int longueur) {
		this.longueur = longueur;
	}

	void initialiserPlateau(int longueur, int largeur) {
		for(int i = 0; i < longueur; i++) {
			for(int j = 0; j < largeur; j++) {
				this.Case[i][j] = "-";
			}
		}
	}
	
	Object getCase(int longueur, int largeur) {
		return this.Case[longueur][largeur];
	}
	
	boolean verifierVieBateau(Bateau bateau) {
		return bateau.vivant;
	}
	
	void ajouterBateau(Bateau bateau) {
		boolean possible = true;
		for(int i = 0; i < bateau.getTaille(); i++) {
			switch(bateau.getOrientationGD()) {
				case "G" : 
					if(this.getCase(bateau.getX() - i, bateau.getY()) != "-")
						possible = false;
				case "D" :
					if(this.getCase(bateau.getX() + i, bateau.getY()) != "-")
						possible = false;
				default : 
					switch(bateau.getOrientationHB()) {
						case "H" :
							if(this.getCase(bateau.getX(), bateau.getY() - i) != "-")
								possible = false;
						case "B" : 
							if(this.getCase(bateau.getX() - i, bateau.getY() + i) != "-")
								possible = false;
						default : 
							possible = false;
					}
			}
		}
		if(possible)
			bateaux.add(bateau); //Le bateau est ajouté à la liste des bateaux du jeu
		else
			System.out.println("Impossible d'ajouter ce bateau");
	}
}
