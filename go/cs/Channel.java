package go.cs;


import go.Observer;

import java.rmi.RemoteException;

import go.Direction;


public class Channel<T> extends java.rmi.server.UnicastRemoteObject implements RemoteChannel<T> {


    public Channel(String name) throws RemoteException {
        // TODO
    }

    public void out(T v) throws RemoteException {
        // TODO
    }
    
    public T in() throws RemoteException {
        // TODO
        return null;
    }

    public String getName() throws RemoteException {
        // TODO
        return null;
    }

    public void observe(Direction direction, Observer observer) throws RemoteException {
        // TODO
    }
}
