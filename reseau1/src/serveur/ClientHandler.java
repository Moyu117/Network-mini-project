package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile String nomJoueur = "Player";
    private volatile SessionJeu session;     // PVP
    private volatile SessionJeuAI sessionAI; // IA
    private volatile String wantedMode = "PVP"; // PVP par défaut

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void setSession(SessionJeu s) { this.session = s; }
    public void setSessionAI(SessionJeuAI s) { this.sessionAI = s; }
    public SessionJeu getSession() { return session; }
    public SessionJeuAI getSessionAI() { return sessionAI; }
    public String getNomJoueur() { return nomJoueur; }

    public void sendMessage(String msg) { out.println(msg); }

    private void closeQuietly() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        sendMessage("WELCOME Bataille-Navale");
        sendMessage("Veuillez vous identifier via UDP avant de jouer (IDENT <pseudo>).");
        sendMessage("Puis sur TCP : NAME <pseudo> et MODE PVP | MODE AI (par défaut PVP).");

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Choix du mode
                if (line.toUpperCase().startsWith("MODE")) {
                    String[] ps = line.split("\\s+");
                    if (ps.length >= 2) {
                        String m = ps[1].toUpperCase();
                        if (m.equals("AI") || m.equals("PVP")) {
                            wantedMode = m;
                            sendMessage("MODE_OK " + wantedMode);
                            if (session == null && sessionAI == null && !"Player".equals(nomJoueur)) {
                                tryStartQueueOrAI();
                            }
                        } else {
                            sendMessage("ERROR Unknown mode. Use: MODE AI | MODE PVP");
                        }
                    } else {
                        sendMessage("ERROR Usage: MODE AI | MODE PVP");
                    }
                    continue;
                }

                // Nom du joueur
                if (line.toUpperCase().startsWith("NAME")) {
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                        nomJoueur = parts[1].trim();

                        // ⚠️ Vérifier que ce pseudo a été identifié en UDP
                        if (!IdentificationUDPServer.isAuthorizedNickname(nomJoueur)) {
                            sendMessage("ERROR Vous devez d'abord vous identifier via UDP (IDENT " + nomJoueur + ").");
                            sendMessage("BYE");
                            break; // On coupe la connexion TCP
                        }

                        sendMessage("HELLO " + nomJoueur);
                        sendMessage("Type SHOT x y lorsque la partie commence.");
                        tryStartQueueOrAI();
                    } else {
                        sendMessage("ERROR Name required. Usage: NAME <nickname>");
                    }
                    continue;
                }

                if (line.equalsIgnoreCase("QUIT")) {
                    sendMessage("BYE");
                    break;
                }

                // Déléguer la commande à la session actuelle (IA ou PVP)
                if (sessionAI != null) {
                    sessionAI.handleCommand(this, line);
                } else if (session != null) {
                    session.handleCommand(this, line);
                } else {
                    sendMessage("WAITING for opponent...");
                }
            }
        } catch (IOException e) {
            // déconnexion
        } finally {
            if (session != null) session.onDisconnect(this);
            if (sessionAI != null) sessionAI.onDisconnect(this);
            closeQuietly();
        }
    }

    private void tryStartQueueOrAI() {
        if ("AI".equalsIgnoreCase(wantedMode)) {
            SessionJeuAI.startFor(this);
        } else {
            SessionJeu.enqueue(this);
        }
    }
}
