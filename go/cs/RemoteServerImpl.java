package go.cs;

import java.util.Map;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class RemoteServerImpl extends UnicastRemoteObject implements RemoteServer {
    
    //vrais canaux sous le capot
    private go.shm.Factory shmFactory;
    
    private Map<String, RemoteChannel<?>> rmiChannels;

    public RemoteServerImpl() throws RemoteException {
        super();
        this.shmFactory = new go.shm.Factory();
        this.rmiChannels = new HashMap<>();
    }

    @Override
    public synchronized <T> RemoteChannel<T> newChannel(String name) throws RemoteException {
        if (rmiChannels.containsKey(name)) {
            return (RemoteChannel<T>) rmiChannels.get(name);
        }
        
        // canal interne shm
        go.Channel<T> shmChan = shmFactory.newChannel(name);
        // et on l'encapsule dans notre objet rmi
        RemoteChannelImpl<T> rmiChan = new RemoteChannelImpl<>(shmChan);
        
        rmiChannels.put(name, rmiChan);
        return rmiChan;
    }
}
