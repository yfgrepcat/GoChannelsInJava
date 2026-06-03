package go.sock;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import go.Direction;


public class Factory implements go.Factory {

    private static volatile Factory INSTANCE;

    private final ConcurrentHashMap<String, go.Channel> cache = new ConcurrentHashMap<>();

    public Factory() {
        INSTANCE = this;
    }

    /** Pour ChannelProxy.readResolve : retrouver la Factory courante. */
    static Factory current() {
        return INSTANCE;
    }

    private String localHost() {
        String h = System.getProperty("go.sock.host");
        if (h != null && !h.isEmpty()) return h;
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    @Override
    public <T> go.Channel<T> newChannel(String name) {
        go.Channel cached = cache.get(name);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            go.Channel<T> typed = (go.Channel<T>) cached;
            return typed;
        }
        // computeIfAbsent garantit qu'on n'enregistre qu'une seule fois côté JVM.
        @SuppressWarnings("unchecked")
        go.Channel<T> result = (go.Channel<T>) cache.computeIfAbsent(name, this::createChannel);
        return result;
    }

    private go.Channel<?> createChannel(String name) {
        ChannelMaster<?> candidate;
        try {
            candidate = new ChannelMaster<>(name);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le ChannelMaster: " + e.getMessage(), e);
        }
        String host = localHost();
        String[] winner = Naming.registerOrLookup(name, host, candidate.getPort());
        if (winner[0].equals(host) && Integer.parseInt(winner[1]) == candidate.getPort()) {
            // On a gagné l'enregistrement : on est le maître.
            return candidate;
        }
        // Quelqu'un d'autre était déjà enregistré : on bascule en esclave.
        candidate.close();
        return new ChannelSlave<>(name, winner[0], Integer.parseInt(winner[1]));
    }

    @Override
    public go.Selector newSelector(Map<go.Channel, Direction> channels) {
        throw new UnsupportedOperationException("select non implémenté en sock");
    }

    @Override
    public go.Selector newSelector(Set<go.Channel> channels, Direction direction) {
        throw new UnsupportedOperationException("select non implémenté en sock");
    }
}
