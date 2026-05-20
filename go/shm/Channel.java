package go.shm;

import go.Direction;
import go.Observer;

public class Channel<T> implements go.Channel<T> {

<<<<<<< Updated upstream
=======
    // Added ReentrantLock and Condition to manage waiting threads 
    // more efficiently than using wait / notifyAll
    private final ReentrantLock lock = new ReentrantLock(true); // true to use fair ordering of threads
    private final Condition waitingSenders = lock.newCondition();
    private final Condition waitingForAck = lock.newCondition();
    private final Condition waitingReceivers = lock.newCondition();

    private final String name;
    private T value;
    private boolean hasValue = false;


    private boolean sendInProgress = false;
    private int receiverCount = 0;

    // Implements the observer pattern for in    and out operations, with separate
    // lists of observers for each direction.
    // see: https://refactoring.guru/design-patterns/observer
    private final List<Observer> inObservers = new ArrayList<>();
    private final List<Observer> outObservers = new ArrayList<>();

>>>>>>> Stashed changes
    public Channel(String name) {
        // TODO
    }
    
    public void out(T v) {
        // TODO
    }
    
    public T in() {
        // TODO
        return null;
    }

    public String getName() {
        // TODO
        return null;
    }

    public void observe(Direction dir, Observer observer) {
        // TODO
    }
        
}
