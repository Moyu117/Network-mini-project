package serveur;

import java.util.Random;

public class JoueurIA {
	 private Random random = new Random();

	    public String makeMove() {
	        int x = random.nextInt(10);
	        int y = random.nextInt(10);
	        return x + "," + y;
	    }
}
