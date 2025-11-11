package Regles;

public class TestBatailleNavale {

	public static void main(String[] args) {
		
		Plateau plateau = new Plateau(10, 10);
        Bateau b1 = new Bateau(2, 2, 3, "D");
        Bateau b2 = new Bateau(5, 5, 4, "B");

        plateau.placerBateau(b1);
        plateau.placerBateau(b2);
        plateau.afficherPlateau();

        plateau.tirer(2, 2);
        plateau.tirer(3, 2);
        plateau.tirer(4, 2);
        plateau.tirer(5, 5);
        plateau.tirer(1, 1);
        plateau.afficherPlateau();

        if (plateau.tousLesBateauxCoules()) {
            System.out.println("Tous les bateaux sont coulés !");
        }
	}

}
