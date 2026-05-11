package go.cs;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implantation d'un serveur hébergeant des canaux.
 *
 * Le serveur agit comme un annuaire de canaux partagés :
 * - Si un canal de nom "X" n'existe pas encore, il est créé.
 * - Si un canal de nom "X" existe déjà, le même objet est retourné.
 *
 * Cela permet à des clients distints (JVMs différentes) de partager
 * le même canal et de se synchroniser via RMI (rendez-vous Go).
 */
public class ServerImpl extends UnicastRemoteObject implements ServerInt {

    /** Annuaire : nom du canal → instance ChannelCS partagée */
    private final Map<String, go.ChannelCS<?>> channels = new HashMap<>();

    /** Port du registry RMI */
    private static final int PORT = 8000;

    /** Nom sous lequel le serveur est enregistré dans le registry */
    private static final String BINDING_NAME = "server";

    protected ServerImpl() throws RemoteException {
        super();
    }

    /**
     * Retourne le canal correspondant au nom donné.
     * Crée le canal s'il n'existe pas encore (lazy creation).
     *
     * synchronized : plusieurs clients peuvent appeler getChannel
     * simultanément, il faut garantir qu'un seul canal est créé par nom.
     */
    @Override
    public synchronized <T> go.ChannelCS<T> getChannel(String name) throws RemoteException {
        if (!channels.containsKey(name)) {
            // Premier appel avec ce nom : on crée le canal et on le stocke
            channels.put(name, new ChannelCS<T>(name));
            System.out.println("[Server] Canal créé : " + name);
        } else {
            System.out.println("[Server] Canal existant retourné : " + name);
        }
        // Le cast est sûr car le type T est paramétrique côté client
        @SuppressWarnings("unchecked")
        go.ChannelCS<T> channel = (go.ChannelCS<T>) channels.get(name);
        return channel;
    }

    /**
     * Point d'entrée du serveur.
     *
     * Étapes :
     *  1. Créer le registry RMI sur le port PORT
     *  2. Instancier ServerImpl (UnicastRemoteObject l'exporte automatiquement)
     *  3. Enregistrer l'instance dans le registry sous BINDING_NAME
     */
    public static void main(String[] args) {
        try {
            // Étape 1 : démarrage du registry RMI sur le port 8000
            Registry registry = LocateRegistry.createRegistry(PORT);
            System.out.println("[Server] Registry RMI démarré sur le port " + PORT);

            // Étape 2 : création de l'instance du serveur
            // (UnicastRemoteObject.exportObject est appelé dans le super())
            ServerImpl server = new ServerImpl();

            // Étape 3 : enregistrement dans le registry
            // Les clients feront : Naming.lookup("rmi://localhost:8000/server")
            registry.rebind(BINDING_NAME, server);
            System.out.println("[Server] Serveur enregistré sous le nom \"" + BINDING_NAME + "\"");
            System.out.println("[Server] En attente de connexions...");

        } catch (RemoteException e) {
            System.err.println("[Server] Erreur RMI : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
