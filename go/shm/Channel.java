package go.shm;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import go.Direction;
import go.Observer;

public class Channel<T> implements go.Channel<T> {

    // Added ReentrantLock and Condition to manage waiting threads more efficiently than using wait / notifyAll
    private final ReentrantLock lock = new ReentrantLock(true); // true to use fair ordering of threads
    private final Condition waitingSenders = lock.newCondition();
    private final Condition waitingForAck = lock.newCondition();
    private final Condition waitingReceivers = lock.newCondition();

    private final String name;
    private T value;
    private boolean hasValue = false;
    private int receiverCount = 0;

    // Implements the observer pattern for in    and out operations, with separate
    // lists of observers for each direction.
    // see: https://refactoring.guru/design-patterns/observer
    private final List<Observer> inObservers = new ArrayList<>();
    private final List<Observer> outObservers = new ArrayList<>();

    public Channel(String name) {
        this.name = name;
    }

    public void out(T v) {
        List<Observer> toNotify = new ArrayList<>();

        lock.lock();
        try {
            while (hasValue) {
                waitingSenders.await();
        }
        value = v;
        hasValue = true;
        // Notify observers of an out operation, then clear the list of observers to
        // notify.
        toNotify.addAll(outObservers);
        outObservers.clear();

        waitingReceivers.signal(); // Signal one waiting receiver

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        // Moved update calls outside of the lock to avoid potential deadlocks if observers call back into the channel.
        for (Observer observer : toNotify) {
            observer.update();
        }

        lock.lock();
        try{
        while (hasValue) {
            waitingForAck.await();
        }
         waitingSenders.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

    }

    public T in() {
        List<Observer> toNotify = new ArrayList<>();
        T res = null;

        lock.lock();
        try {
            receiverCount++;

            toNotify.addAll(inObservers); // Gather observers to notify
            inObservers.clear(); // Clear the list of observers to notify, as they will be notified now.

            for (Observer observer : toNotify) {
                observer.update();
            }

            while(!hasValue) {
                waitingReceivers.await();
            }

            res = value;
            hasValue = false;
            receiverCount--;

            waitingForAck.signal(); // Signal one waiting sender
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        return res;
    }

    public String getName() {
        return name;
    }

    public void observe(Direction direction, Observer observer) {
        lock.lock();
        try {
            if (direction == Direction.In && receiverCount > 0) {
                observer.update();
            } else if (direction == Direction.Out && hasValue) {
                observer.update();
            } else {
                if (direction == Direction.In) {
                    inObservers.add(observer);
                } else {
                    outObservers.add(observer);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
