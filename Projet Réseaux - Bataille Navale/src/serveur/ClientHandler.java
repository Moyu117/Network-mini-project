package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Interagissez avec un seul client: lisez la commande et transmettez-la à SessionJeu
 * protocole (ligne de texte)：
 *  - client：NAME <nick>
 *  - client：SHOT x y
 *  - client：QUIT
 *  - client：WELCOME / WAITING / MATCHED <op> / YOUR_TURN / OPPONENT_TURN / RESULT <...> / OPPONENT_<...>
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile String nomJoueur = "Player";
    private volatile SessionJeu session;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.out = new PrintWriter(socket.getOutputStream(), true); // autoflush
    }

    public void setSession(SessionJeu session) { this.session = session; }
    public SessionJeu getSession() { return session; }
    public String getNomJoueur() { return nomJoueur; }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    private void closeQuietly() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        sendMessage("WELCOME Bataille-Navale");
        sendMessage("Please identify: NAME <nickname>");
        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.toUpperCase().startsWith("NAME")) {
                    // NAME <nick>
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                        nomJoueur = parts[1].trim();
                        sendMessage("HELLO " + nomJoueur);
                        sendMessage("Type SHOT x y when your game starts.");
                        // Rejoindre la file d'attente correspondante
                        SessionJeu.enqueue(this);
                    } else {
                        sendMessage("ERROR Name required. Usage: NAME <nickname>");
                    }
                    continue;
                }

                if (line.equalsIgnoreCase("QUIT")) {
                    sendMessage("BYE");
                    break;
                }

                // Transférez la commande du jeu à Session
                SessionJeu s = session;
                if (s != null) {
                    s.handleCommand(this, line);
                } else {
                    sendMessage("WAITING for opponent...");
                }
            }
        } catch (IOException e) {
            // deconnetion
        } finally {
            if (session != null) session.onDisconnect(this);
            closeQuietly();
        }
    }
}
