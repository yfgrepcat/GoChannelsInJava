package go.sock;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Naming {

    private static final int DEFAULT_PORT = 1099;
    private static final ConcurrentHashMap<String, String[]> registry = new ConcurrentHashMap<>();

    public static String[] registerOrLookup(String name, String host, int port) {
        String namingHost = System.getProperty("go.sock.naming.host", "localhost");
        int namingPort = Integer.parseInt(System.getProperty("go.sock.naming.port", String.valueOf(DEFAULT_PORT)));

        try (Socket socket = new Socket(namingHost, namingPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("REGISTER " + name + " " + host + " " + port);
            String response = in.readLine();
            if (response != null && response.startsWith("OK ")) {
                String[] parts = response.substring(3).split(" ");
                if (parts.length == 2) {
                    return parts;
                }
            }
            throw new RuntimeException("Naming server bad response: " + response);
        } catch (IOException e) {
            throw new RuntimeException("Cannot contact Naming server", e);
        }
    }

    public static void main(String args[]) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            String p = System.getProperty("go.sock.naming.port");
            if (p != null) port = Integer.parseInt(p);
        }
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Naming server started on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            
            String line = in.readLine();
            if (line != null && line.startsWith("REGISTER ")) {
                String[] parts = line.split(" ");
                if (parts.length == 4) {
                    String name = parts[1];
                    String host = parts[2];
                    String portStr = parts[3];
                    
                    String[] existing = registry.putIfAbsent(name, new String[]{host, portStr});
                    if (existing != null) {
                        out.println("OK " + existing[0] + " " + existing[1]);
                    } else {
                        out.println("OK " + host + " " + portStr);
                    }
                } else {
                    out.println("ERROR bad format");
                }
            } else {
                out.println("ERROR unknown command");
            }
        } catch (IOException e) {
            // ignore client errors
        } finally {
            try { client.close(); } catch (IOException e) {}
        }
    }
}
