package go.cs;

import go.Direction;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/* côté client - cache tt la complexité réseau */
public class Factory implements go.Factory {

    private RemoteServer remoteServer;

    public Factory() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            this.remoteServer = (RemoteServer) registry.lookup("GoServer");
        } catch (Exception e) {
            System.err.println("pas trouver le serveur RMI");
            throw new RuntimeException("Impossible de se connecter au serveur", e);
        }
    }

    /** Création ou accès à un canal existant.
     * Côté serveur, le canal est créé au premier appel avec un nom donné ;
     * les appels suivants avec le même nom donneront accès au même canal.
     */
    public <T> go.Channel<T> newChannel(String name) {
        try {
            RemoteChannel<T> rChan = remoteServer.newChannel(name);

            return new Channel<>(rChan);
        } catch (Exception e) {
            throw new RuntimeException("Erreur à la création du canal", e);
        }
    }
    
    /** Spécifie quels sont les canaux écoutés et la direction pour chacun. */
    public go.Selector newSelector(Map<go.Channel, Direction> channels) {
       return new Selector(channels);
    }

    /** Spécifie quels sont les canaux écoutés et la même direction pour tous. */
    public go.Selector newSelector(Set<go.Channel> channels, Direction direction) {
        return newSelector(channels
                           .stream() 
                           .collect(Collectors.toMap(Function.identity(), e -> direction)));
    }

}

