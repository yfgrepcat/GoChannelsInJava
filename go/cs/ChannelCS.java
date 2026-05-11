package go.cs;

import java.util.*;
import java.rmi.RemoteException;

import go.Direction;
import go.Observer;

public class ChannelCS<T> extends java.rmi.server.UnicastRemoteObject implements go.ChannelCS<T> {

    private final String name;
    private T message;

    // canal Go (non bufferisé) -> 2 attributs pour rdv
    private boolean messPresent = false;
    private boolean messRecu = false;

    // Côté observation
    private int recepEnAttente = 0; //

    // Partie Observers
    private List<Observer> observerRecep = new ArrayList<>();
    private List<Observer> observerEmet = new ArrayList<>();

    public ChannelCS(String name) throws RemoteException {
        super();
        this.name = name;
    }

    public synchronized void out(T v) throws RemoteException {

        // Gestion émetteurs : bloque si canal occupé
        while (messPresent) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        this.message = v;
        this.messPresent = true;
        this.messRecu = false;

        // Notifie les obs récepteurs d'un message
        for (Observer obs : observerRecep) {
            obs.update();
        }
        observerRecep.clear();

        notifyAll();

        // Attente d'un récepteur
        while (!messRecu) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        this.messPresent = false;
        notifyAll(); // réveille les autres émetteurs
    }

    public synchronized T in() throws RemoteException {

        recepEnAttente++;

        // même principe avec les récepteurs
        for (Observer obs : observerEmet) {
            obs.update();
        }
        observerEmet.clear();

        while (!messPresent || messRecu) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        recepEnAttente--;

        T contenu = this.message;
        this.messRecu = true;
        notifyAll(); // signale en face

        return contenu;
    }

    public String getName() throws RemoteException {
        return this.name;
    }

    public synchronized void observe(Direction direction, Observer observer) throws RemoteException {

        // 2cas: en cours ou pas encore
        if (direction == Direction.In) {
            if (messPresent && !messRecu) {
                observer.update();
            } else { // personne au niveau de la réception
                observerRecep.add(observer);
            }

        } else if (direction == Direction.Out) {
            if (recepEnAttente > 0) {
                observer.update();
            } else { // personne au niveau de l'émission
                observerEmet.add(observer);
            }
        }
        // facto en map à voir????
    }
}
