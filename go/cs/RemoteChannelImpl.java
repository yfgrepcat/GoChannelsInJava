package go.cs;

import go.Observer;
import go.Direction;
import java.util.Map;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

/* vit uniquement sur le serv, appelle le notre version shm */
public class RemoteChannelImpl<T> extends UnicastRemoteObject implements RemoteChannel<T> {
    
    // correspondance obs distant - locaux
    private Map<RemoteObserver, Observer> observersMap;
    private go.shm.Channel<T> shmChannel;
    
    public RemoteChannelImpl(go.Channel<T> shmChannel) throws RemoteException {
        super();
        this.shmChannel = (go.shm.Channel<T>) shmChannel;
        this.observersMap = new ConcurrentHashMap<>();
    }
    /*hasmap résiste multi-threading */
    /*plus opti qu'un bloc synchronized */

    @Override
    public void out(T v) throws RemoteException {
        shmChannel.out(v);
    }

    @Override
    public T in() throws RemoteException {
        return shmChannel.in();
    }

    @Override
    public String getName() throws RemoteException {
        return shmChannel.getName();
    }
    
    @Override
    public void observe(Direction d, RemoteObserver ro) throws RemoteException {
        // le pont - notre wrapper 
        // shmChannel accepte que des go.Observer pas RemoteObserver
        Observer localObserver = new Observer() {
            @Override
            public void update() {
                try {
                    ro.update(); // callback RMI vers client 
                } catch (RemoteException e) { 
                    System.err.println("impossible de joindre le client pour le notifier.");
                }
            }
        };

        // correspondance
        // indispensable pour le unobserve plus tard
        observersMap.put(ro, localObserver);
    
        shmChannel.observe(d, localObserver);
    }

    @Override
    public void unobserve(Direction d, RemoteObserver ro) throws RemoteException {
        Observer localObserver = observersMap.get(ro);
        if (localObserver != null) {
            shmChannel.unobserve(d, localObserver);
            observersMap.remove(ro);
        }
    }
}

