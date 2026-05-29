package go.sock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import go.Direction;
import go.Observer;

/** Côté esclave d'un canal sock : ouvre une nouvelle connexion vers le
 *  ChannelMaster pour chaque opération in/out (cf. choix de conception :
 *  "one socket per call" — simple, parallèle naturellement entre threads,
 *  cohérent avec la nature bloquante des opérations).
 */
public class ChannelSlave<T> implements go.Channel<T> {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String host;
    private final int port;

    public ChannelSlave(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    @Override
    public void out(T v) {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeByte(ChannelMaster.OP_OUT);
            out.writeObject(v);
            out.flush();
            byte ack = in.readByte();
            if (ack != ChannelMaster.ACK) {
                throw new IOException("ACK inattendu: " + ack);
            }
        } catch (IOException e) {
            throw new RuntimeException("ChannelSlave[" + name + "].out: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T in() {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeByte(ChannelMaster.OP_IN);
            out.flush();
            T v = (T) in.readObject();
            // ACK : indispensable pour que le producteur (côté maître) ne
            // soit libéré qu'une fois la valeur effectivement réceptionnée.
            out.writeByte(ChannelMaster.ACK);
            out.flush();
            return v;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("ChannelSlave[" + name + "].in: " + e.getMessage(), e);
        }
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
