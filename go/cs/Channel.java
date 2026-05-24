package go.cs;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import go.Direction;
import go.Observer;

public class Channel<T> implements go.Channel<T> {

    private Map<Observer, RemoteObserver> observersMap;
    private RemoteChannel<T> remoteChan;


    public Channel(RemoteChannel<T> remoteChan) {
        this.remoteChan = remoteChan;
        this.observersMap = new ConcurrentHashMap<>();
    }

    public void out(T v) {
        try {
            remoteChan.out(v);
        } catch (RemoteException e) {
            throw new RuntimeException("Erreur réseau out", e);
        }
    }
    
    public T in() {
        try {
            return remoteChan.in();
        } catch (RemoteException e) {
            throw new RuntimeException("Erreur réseau in", e);
        }
    }

    public String getName() {
        try {
            return remoteChan.getName();
        } catch (RemoteException e) {
            throw new RuntimeException("Erreur réseau getName", e);
        }
    }

    public void observe(Direction direction, Observer observer) {
        try {
            // on emballe l'obs local dans un obj réseau
            RemoteObserver ro = new RemoteObserverImpl(observer);
            observersMap.put(observer, ro);
            
            // on l'envoie au serv
            remoteChan.observe(direction, ro);
        } catch (RemoteException e) {
            throw new RuntimeException("Erreur réseau observe", e);
        }
    }

    public void unobserve(Direction direction, Observer observer) {
        try {
            RemoteObserver ro = observersMap.get(observer);
            if (ro != null) {
                remoteChan.unobserve(direction, ro);
                observersMap.remove(observer);
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Erreur réseau désabonnement", e);
        }
    }
}
