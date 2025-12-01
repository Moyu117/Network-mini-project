package serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurJeu {

    public static void main(String[] args) {
        int portTCP = 12345;
        int portUDP = 12346;

        System.out.println("=== Serveur Bataille Navale (TCP " + portTCP + ", UDP " + portUDP + ") ===");

        // Lance le serveur UDP d'identification (thread daemon)
        Thread udpThread = new Thread(new IdentificationUDPServer(portUDP), "UDP-IDENT");
        udpThread.setDaemon(true);
        udpThread.start();

        // Serveur TCP pour la partie jeu (PVP / IA)
        try (ServerSocket serverSocket = new ServerSocket(portTCP)) {
            System.out.println("[TCP] Serveur en écoute sur le port " + portTCP);
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[TCP] Nouveau client : " + client.getInetAddress() + ":" + client.getPort());
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler, "Client-" + client.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Erreur serveur TCP : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
