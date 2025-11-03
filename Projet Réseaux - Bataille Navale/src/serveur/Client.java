package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client console simple pour Bataille Navale PVP.
 * Usage : java serveur.Client
 *   saisir：
 *   NAME Alice
 *   SHOT 3 5
 *   QUIT
 */
public class Client {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 12345;

        System.out.println("=== Client Bataille Navale ===");
        System.out.println("(connecte à " + host + ":" + port + ")");

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // Thread : lire les messages du serveur et les afficher
            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("[Serveur] " + line);
                    }
                } catch (IOException e) {
                    System.out.println("❌ Déconnecté du serveur.");
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            // Thread principal : envoyer les entrées de l’utilisateur
            String input;
            while ((input = console.readLine()) != null) {
                input = input.trim();
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    out.println("QUIT");
                    break;
                }
                out.println(input);
            }

        } catch (IOException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }
}
