package go.cs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInt extends java.rmi.Remote {

    public <T> go.ChannelCS<T> getChannel(String name) throws RemoteException;

}
