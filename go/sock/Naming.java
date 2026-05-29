package go.sock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/** Service de nommage du package sock.
 *
 *  Rôle : associer un nom de canal à l'adresse (host, port) de son
 *  ChannelMaster. Démarré comme un processus indépendant (main), il
 *  écoute sur un port TCP fixe et répond à des requêtes texte.
 *
 *  Protocole (une ligne par requête, une ligne par réponse) :
 *    REGISTER <name> <host> <port>   -> OK <host> <port>
 *  Sémantique « register-or-lookup atomique » :
 *    - si <name> est libre, on enregistre (host,port) et on renvoie
 *      cette même paire (le client a gagné la course, il est le maître);
 *    - sinon on renvoie l'entrée déjà enregistrée (le client devient
 *      esclave et sait à qui se connecter).
 *  L'atomicité est portée par ConcurrentHashMap.putIfAbsent, ce qui
 *  évite la course entre deux JVM qui appellent newChannel(name) en
 *  même temps.
 *
 *  Configuration : -Dgo.sock.naming.host=... / -Dgo.sock.naming.port=...
 */
public class Naming {

    private static final int DEFAULT_PORT = 2999;

    public static int port() {
        String p = System.getProperty("go.sock.naming.port");
        return (p != null && !p.isEmpty()) ? Integer.parseInt(p) : DEFAULT_PORT;
    }

    public static String host() {
        String h = System.getProperty("go.sock.naming.host");
        return (h != null && !h.isEmpty()) ? h : "localhost";
    }

    /** Appelé par {@link Factory#newChannel}. Renvoie l'adresse du maître
     *  effectif sous la forme {host, port}. Si l'appelant gagne la course,
     *  la réponse est sa propre adresse. */
    public static String[] registerOrLookup(String name, String host, int port) {
        try (Socket s = new Socket(host(), port());
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            out.println("REGISTER " + name + " " + host + " " + port);
            String response = in.readLine();
            if (response == null) {
                throw new IOException("service de nommage a fermé la connexion");
            }
            String[] parts = response.split(" ");
            if (parts.length != 3 || !"OK".equals(parts[0])) {
                throw new IOException("réponse inattendue: " + response);
            }
            return new String[] { parts[1], parts[2] };
        } catch (IOException e) {
            throw new RuntimeException(
                "Naming indisponible à " + host() + ":" + port() + " : " + e.getMessage(), e);
        }
    }

    public static void main(String args[]) throws Exception {
        ConcurrentHashMap<String, String[]> registry = new ConcurrentHashMap<>();
        int port = port();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Naming en écoute sur le port " + port);
            while (true) {
                Socket client = server.accept();
                Thread h = new Thread(() -> handle(client, registry), "naming-handler");
                h.setDaemon(true);
                h.start();
            }
        }
    }

    private static void handle(Socket client, ConcurrentHashMap<String, String[]> registry) {
        try (Socket s = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            String line = in.readLine();
            if (line == null) return;
            String[] parts = line.split(" ");
            if (parts.length == 4 && "REGISTER".equals(parts[0])) {
                String name = parts[1];
                String[] candidate = { parts[2], parts[3] };
                String[] existing = registry.putIfAbsent(name, candidate);
                String[] winner = (existing != null) ? existing : candidate;
                out.println("OK " + winner[0] + " " + winner[1]);
            } else {
                out.println("ERR mauvaise requête");
            }
        } catch (IOException e) {
            // client parti ou réseau cassé : on laisse tomber la connexion.
        }
    }
}
