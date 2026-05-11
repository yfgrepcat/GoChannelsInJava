package go.shm;

import java.util.ArrayList;
import java.util.List;

import go.Direction;
import go.Observer;

public class Channel<T> implements go.Channel<T> {

    private final String name;
    private T value;
    private boolean hasValue = false;
    private int waitingReceivers = 0;

    // Implements the observer pattern for in and out operations, with separate
    // lists of observers for each direction.
    // see: https://refactoring.guru/design-patterns/observer
    private final List<Observer> inObservers = new ArrayList<>();
    private final List<Observer> outObservers = new ArrayList<>();

    public Channel(String name) {
        this.name = name;
    }

    public void out(T v) {
        List<Observer> toNotify = new ArrayList<>();

        synchronized (this) {

            while (hasValue) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            value = v;
            hasValue = true;
            // Notify observers of an out operation, then clear the list of observers to
            // notify.
            toNotify.addAll(inObservers);
            inObservers.clear();

            notifyAll();

            while (hasValue) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        for (Observer observer : toNotify) {
            observer.update();
        }
    }

    public T in() {
        List<Observer> toNotify = new ArrayList<>();
        T res;

        synchronized (this) {
            waitingReceivers++;

            // Gathers observers to notify, then clear the list of observers to notify.
            toNotify.addAll(outObservers);
            outObservers.clear();

            while (!hasValue) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            res = value;
            hasValue = false;
            waitingReceivers--;

            // Temporary notifyAll
            notifyAll();
        }

        for (Observer observer : toNotify) {
            observer.update();
        }

        return res;
    }

    public String getName() {
        return name;
    }

    public void observe(Direction direction, Observer observer) {
        if (direction == Direction.In) {
            synchronized (this) {
                if (hasValue) {
                    observer.update();
                } else {
                    // Observe if no data yet
                    inObservers.add(observer);
                }
            }
        } else {
            synchronized (this) {
                // Observe if no receiver yet
                if (waitingReceivers > 0) {
                    observer.update();
                } else {
                    outObservers.add(observer);
                }
            }
        }
    }
}
