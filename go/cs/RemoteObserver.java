package go.cs;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface RMI permet au serv de faire un callback 
// vers le client
public interface RemoteObserver extends Remote {
    public void update() throws RemoteException;
}
