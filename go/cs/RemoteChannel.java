package go.cs;

import go.Direction;
import java.rmi.Remote;
import java.rmi.RemoteException;

/* contrat - architecture RMI (méthodes vont êtres appelées à travers le réseau) */
public interface RemoteChannel<T> extends java.rmi.Remote {
    
    public void out(T v) throws RemoteException;
    public T in() throws RemoteException;
    public String getName() throws RemoteException;
    
    public void observe(Direction d, RemoteObserver ro) throws RemoteException;
    public void unobserve(Direction d, RemoteObserver ro) throws RemoteException;
}

