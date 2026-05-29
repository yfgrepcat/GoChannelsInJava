package go.sock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import go.Direction;
import go.Observer;

/** Côté maître d'un canal sock : un ServerSocket accepte les requêtes
 *  IN/OUT envoyées par les esclaves, et délègue à un canal shm interne
 *  pour la synchronisation effective. Implémente go.Channel<T> pour que
 *  les utilisateurs sur la JVM maître puissent aussi appeler in/out
 *  directement.
 *
 *  Sémantique synchrone bout-en-bout : out(v) bloque jusqu'à ce que la
 *  valeur ait été *réellement* récupérée par le consommateur — local ou
 *  distant. Pour le cas distant, on glisse chaque valeur dans une enveloppe
 *  porteuse d'un CountDownLatch ; le handler du maître ne le décrémente
 *  qu'après réception d'un ACK de l'esclave. Sans ce mécanisme, la JVM
 *  productrice pourrait sortir avant que le handler n'ait poussé les octets
 *  sur le réseau.
 */
public class ChannelMaster<T> implements go.Channel<T> {

    private static final long serialVersionUID = 1L;

    static final byte OP_IN = 0;
    static final byte OP_OUT = 1;
    static final byte ACK = 42;

    private static final class Envelope<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        final T value;
        final CountDownLatch delivered;
        Envelope(T value) {
            this.value = value;
            this.delivered = new CountDownLatch(1);
        }
    }

    private final String name;
    private final go.shm.Channel<Envelope<T>> innerShm;
    private final ServerSocket serverSocket;
    private final int port;

    public ChannelMaster(String name) throws IOException {
        this.name = name;
        this.innerShm = new go.shm.Channel<>(name);
        this.serverSocket = new ServerSocket(0);
        this.port = serverSocket.getLocalPort();
        Thread acceptor = new Thread(this::acceptLoop, "sock-master-" + name);
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public int getPort() {
        return port;
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                Thread h = new Thread(() -> handle(client), "sock-master-" + name + "-handler");
                h.setDaemon(true);
                h.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("ChannelMaster[" + name + "] accept: " + e.getMessage());
                }
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handle(Socket client) {
        try (Socket s = client;
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            byte op = in.readByte();
            if (op == OP_IN) {
                Envelope<T> env = innerShm.in();
                out.writeObject(env.value);
                out.flush();
                byte ack = in.readByte();          // bloque sur l'ACK esclave
                if (ack != ACK) {
                    System.err.println("ChannelMaster[" + name + "] ACK IN inattendu: " + ack);
                }
                env.delivered.countDown();         // libère enfin le producteur
            } else if (op == OP_OUT) {
                T v = (T) in.readObject();
                // out() local bloquera jusqu'à ce qu'un consommateur ait pris ET acké.
                out(v);
                out.writeByte(ACK);
                out.flush();
            } else {
                System.err.println("ChannelMaster[" + name + "] op inconnu: " + op);
            }
        } catch (IOException | ClassNotFoundException e) {
            // Client parti ou erreur réseau : on laisse mourir la connexion.
        }
    }

    /** Fermeture du maître (utilisée par la Factory si on perd la course
     *  au registre et qu'on doit basculer en esclave). */
    void close() {
        try { serverSocket.close(); } catch (IOException ignored) {}
    }

    @Override
    public void out(T v) {
        Envelope<T> env = new Envelope<>(v);
        innerShm.out(env);
        boolean interrupted = false;
        while (env.delivered.getCount() > 0) {
            try {
                env.delivered.await();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    @Override
    public T in() {
        Envelope<T> env = innerShm.in();
        env.delivered.countDown();
        return env.value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void observe(Direction direction, Observer observer) {
        throw new UnsupportedOperationException("observe non implémenté en sock");
    }

    private Object writeReplace() {
        return new ChannelProxy(name);
    }
}
