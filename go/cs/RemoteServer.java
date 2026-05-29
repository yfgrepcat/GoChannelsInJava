package go.cs;

import java.rmi.Remote;
import java.rmi.RemoteException;

// contrat RMI pour le serveur (donne accès canaux)
public interface RemoteServer extends Remote {
    public <T> RemoteChannel<T> newChannel(String name) throws RemoteException;
}
