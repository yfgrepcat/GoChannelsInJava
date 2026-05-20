package go.cs;

import go.Observer;
import go.Direction;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RemoteChannel<T> extends Remote {

    public void out(T v) throws RemoteException;

    public T in() throws RemoteException;

    public String getName() throws RemoteException;

    public void observe(Direction direction, Observer observer) throws RemoteException;
}
