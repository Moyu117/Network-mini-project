package Règles;
import java.util.ArrayList;

public class Plateau {
	
	private char[][] grille;
	int largeur;
	int longueur;
	ArrayList<Bateau> bateaux = new ArrayList<Bateau>();
	
	public Plateau(int longueur, int largeur) {
		this.grille = new char[longueur][largeur];
		this.largeur = largeur;
		this.longueur = longueur;
		this.bateaux = new ArrayList<>();
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
				this.grille[i][j] = '-';
			}
		}
	}
	
	Object getGrille(int longueur, int largeur) {
		return this.grille[longueur][largeur];
	}
	
	boolean verifierVieBateau(Bateau bateau) {
		return bateau.vivant;
	}
	
	public boolean placerBateau(Bateau bateau) {
        ArrayList<int[]> positions = new ArrayList<>();
        int x = bateau.getX();
        int y = bateau.getY();

        for (int i = 0; i < bateau.getTaille(); i++) {
            int nx = x, ny = y;

            switch (bateau.getOrientation()) {
                case "H": ny = y - i; break;
                case "B": ny = y + i; break;
                case "G": nx = x - i; break;
                case "D": nx = x + i; break;
            }

            // Vérifie les limites du plateau
            if (nx < 0 || nx >= longueur || ny < 0 || ny >= largeur) {
                System.out.println("Construction impossible : le bateau dépasse du plateau.");
                return false;
            }

            // Vérifie la présence d’un autre bateau
            if (grille[nx][ny] != '-') {
                System.out.println("Construction impossible : case déjà occupée.");
                return false;
            }

            positions.add(new int[]{nx, ny});
        }

        // Place le bateau
        for (int[] pos : positions) {
            grille[pos[0]][pos[1]] = 'B';
        }
        bateau.setPositions(positions);
        bateaux.add(bateau);
        return true;
    }
	
	public boolean tirer(int x, int y) {
		System.out.println("Tir en (" + x + "," + y + ")");
		
		if (x < 0 || x >= longueur || y < 0 || y >= largeur) {
			System.out.println("Tir en dehors du plateau !");
			return false;
	    }

		if (grille[x][y] == 'B') {
			grille[x][y] = 'X';
			System.out.println("Touché !");
			verifierEtatBateaux();
			return true;
	    } else if (grille[x][y] == '-') {
	    	grille[x][y] = 'O';
	    	System.out.println("Manqué !");
	    } else {
	    	System.out.println("Cette case a déjà été visée.");
	    }
		return false;
	}

	private void verifierEtatBateaux() {
		for (Bateau b : bateaux) {
			boolean coule = true;
			for (int[] pos : b.getPositions()) {
				if (grille[pos[0]][pos[1]] == 'B') {
					coule = false;
					break;
				}
			}
			if (coule && b.isVivant()) {
				b.setVivant(false);
				System.out.println("Un bateau a coulé !");
			}
		}
	}

	public boolean tousLesBateauxCoules() {
		for (Bateau b : bateaux) {
			if (b.isVivant()) return false;
		}
		return true;
	}

	public void afficherPlateau() {
		System.out.print("  ");
		for (int j = 0; j < largeur; j++) {
			System.out.print(j + " ");
		}
		System.out.println();

		for (int i = 0; i < longueur; i++) {
			System.out.print(i + " ");
			for (int j = 0; j < largeur; j++) {
				System.out.print(grille[i][j] + " ");
	        }
			System.out.println();
		}
	}

}
