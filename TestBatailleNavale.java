
public class TestBatailleNavale {

	public static void main(String[] args) {
		Plateau plateau = new Plateau(15, 15);
		
		String affichage = "";
		
		for(int i = 0; i < plateau.getLargeur(); i++) {
			affichage = affichage + "Ligne " + i + " : ";
			for(int j = 0; j < plateau.getLongueur(); j++) {
				affichage = affichage + " " + plateau.getCase(i, j).toString() + " ";
			}
			affichage = affichage + "\n";
		}
		
		System.out.println(affichage);
	}

}
