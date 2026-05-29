package go.cs;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import go.Observer;

// L'objet RMI qui va vivre chez le client 
// et attendre le callback du serveur
public class RemoteObserverImpl extends UnicastRemoteObject implements RemoteObserver {
    
    private Observer localObserver;

    public RemoteObserverImpl(Observer localObserver) throws RemoteException {
        super();
        this.localObserver = localObserver;
    }

    @Override
    public void update() throws RemoteException {
        localObserver.update();
    }
}
