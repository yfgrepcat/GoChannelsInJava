package go.cs;

import go.Direction;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Factory implements go.Factory {

    // ServerInt server = (ServerInt)
    // java.rmi.Naming.lookup("rmi://");

    /**
     * Création ou accès à un canal existant.
     * Côté serveur, le canal est créé au premier appel avec un nom donné ;
     * les appels suivants avec le même nom donneront accès au même canal.
     */
    public <T> go.Channel<T> newChannel(String name) {

        try {
            ServerInt server = (ServerInt) java.rmi.Naming.lookup("rmi://localhost:8000/server");
            return server.getChannel(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /** Spécifie quels sont les canaux écoutés et la direction pour chacun. */
    public go.Selector newSelector(Map<go.Channel, Direction> channels) {
        Map<ChannelCS, Direction> csMap = new java.util.HashMap<>();

        for (Map.Entry<go.Channel, Direction> entry : channels.entrySet()) {
            ChannelCS chan = (ChannelCS) entry.getKey();
            Direction dir = entry.getValue();
            csMap.put(chan, dir);
        }

        return new Selector(csMap);
    }

    /** Spécifie quels sont les canaux écoutés et la même direction pour tous. */
    public go.Selector newSelector(Set<go.Channel> channels, Direction direction) {
        return newSelector(channels
                .stream()
                .collect(Collectors.toMap(Function.identity(), e -> direction)));
    }

}
