package go.cs;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Implantation d'un serveur hébergeant des canaux.
 *
 */

public class ServerImpl {

    public static void main(String args[]) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            
            RemoteServerImpl server = new RemoteServerImpl();
            
            registry.rebind("GoServer", server);
            
            System.out.println("serv - Prêt");
        } catch (Exception e) {
            System.err.println("erreur - lancement du serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
