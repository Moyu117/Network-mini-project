package serveur;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Petit serveur UDP d'identification.
 * Protocole très simple :
 *   client -> "IDENT <pseudo>"
 *   serveur -> "IDENT_OK <pseudo>" ou "IDENT_ERROR ..."
 *
 * Les pseudos validés sont mémorisés dans un Set static, consulté côté TCP.
 */
public class IdentificationUDPServer implements Runnable {

    private final int port;
    private static final Set<String> authorized =
            Collections.synchronizedSet(new HashSet<>());

    public IdentificationUDPServer(int port) {
        this.port = port;
    }

    public static void authorize(String nick) {
        if (nick != null && !nick.isBlank()) {
            authorized.add(nick.trim());
        }
    }

    public static boolean isAuthorizedNickname(String nick) {
        return nick != null && authorized.contains(nick.trim());
    }

    @Override
    public void run() {
        System.out.println("[UDP] Serveur d'identification démarré sur le port " + port);
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[512];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String req = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                String resp;

                if (req.toUpperCase().startsWith("IDENT")) {
                    String[] parts = req.split("\\s+", 2);
                    if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                        String nick = parts[1].trim();
                        authorize(nick);
                        System.out.println("[UDP] IDENT reçu pour pseudo '" + nick
                                + "' depuis " + packet.getAddress() + ":" + packet.getPort());
                        resp = "IDENT_OK " + nick;
                    } else {
                        resp = "IDENT_ERROR Nom manquant";
                    }
                } else {
                    resp = "IDENT_ERROR Commande inconnue";
                }

                byte[] outData = resp.getBytes(StandardCharsets.UTF_8);
                DatagramPacket outPacket = new DatagramPacket(
                        outData, outData.length, packet.getAddress(), packet.getPort());
                socket.send(outPacket);
            }
        } catch (IOException e) {
            System.err.println("[UDP] Erreur serveur UDP : " + e.getMessage());
        }
    }
}
