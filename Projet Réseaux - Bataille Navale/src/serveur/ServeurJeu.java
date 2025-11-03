package serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Serveur PVP concurrent (TCP).
 * - Chaque connexion -> ClientHandler pile
 * - Faites correspondre deux joueurs dans la file d'attente de SessionJeu
 */
public class ServeurJeu {
    public static final int PORT = 12345;

    // option:pour statistique/gestion
    private static final List<ClientHandler> clients =
            Collections.synchronizedList(new LinkedList<>());

    public static void main(String[] args) {
        System.out.println("=== Serveur Bataille Navale PVP @ port " + PORT + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket s = serverSocket.accept();
                ClientHandler ch = new ClientHandler(s);
                clients.add(ch);
                new Thread(ch, "ClientHandler-" + s.getRemoteSocketAddress()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ClientHandler> getClients() {
        return clients;
    }
}
