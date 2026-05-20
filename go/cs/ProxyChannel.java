package go.cs;

import go.Observer;
import go.Direction;
import java.rmi.Remote

public class ProxyChannel implements go.Channel<T> {

    // Interface go.channel immuable -> bypass via envoi d'un stub RMI

    private RemoteChannel<T> canalRemote;

     public ProxyChannel(RemoteChannel<T> canalRemote) {
        this.canalRemote = canalRemote;
    }

    @Override
    public void out(T v) {
        try {
            canalRemote.out(v);
        } catch (java.rmi.RemoteException e) {
            throw new RuntimeException("erreur réseau (out)" + e.getMessage());
        }
    }

    @Override
    public T in() {
        try {
            return canalRemote.in();
        } catch (java.rmi.RemoteException e) {
            throw new RuntimeException("erreur réseau (in)" + e.getMessage());
        }
    }

    @Override
    public String getName() {
        try {
            return canalRemote.getName();
        } catch (java.rmi.RemoteException e) {
            throw new RuntimeException("erreur réseau (getName)" + e.getMessage());
        }
    }

    @Override
    public void observe(Direction direction, Observer observer) {
        try {
            canalRemote.observe(direction, observer);
        } catch (java.rmi.RemoteException e) {
            throw new RuntimeException("erreur réseau (observe)" + e.getMessage());
        }
    }
}
