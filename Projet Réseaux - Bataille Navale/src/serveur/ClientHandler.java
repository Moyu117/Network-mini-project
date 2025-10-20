package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nomJoueur;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bienvenue sur le serveur de Bataille Navale !");
            out.println("Entrez votre pseudo : ");
            nomJoueur = in.readLine();

            out.println("Choisissez un mode :\n1) Jouer contre l'ordinateur\n2) Jouer contre un autre joueur");
            String mode = in.readLine();

            if (mode.equals("1")) {
                SessionJeu session = new SessionJeu(this, new JoueurIA());
                session.start();
            } else if (mode.equals("2")) {
                out.println("En attente d'un autre joueur...");
                ServeurJeu.getClients().remove(this); // évite de se retrouver dans la liste d’attente
                SessionJeu.ListeAttente.add(this);
            } else {
                out.println("Mode invalide. Connexion terminée.");
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNomJoueur() {
        return this.nomJoueur;
    }

    public BufferedReader getInput() {
    	return in;
    }
    
    public PrintWriter getOutput() { 
    	return out;
    }
}