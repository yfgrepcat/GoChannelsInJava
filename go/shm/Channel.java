package go.shm;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import go.Direction;
import go.Observer;

public class Channel<T> implements go.Channel<T>, go.ChannelInterfaceDev<T> {

    // senderLock serialises concurrent out() calls so that at most one sender
    // is "in transit" at a time. Without this, the gap between setting
    // hasValue=true (block 1) and waiting on waitingForAck (block 2) lets a
    // second sender slip in and overwrite hasValue. Then both senders end up
    // awaiting waitingForAck and signal() (woken by a single in()) only
    // releases one of them: the other deadlocks. Serialising senders keeps a
    // single waiter on waitingForAck, so signal() is sufficient.
    private final ReentrantLock senderLock = new ReentrantLock(true);

    // stateLock protects channel state (value, hasValue, observer lists,
    // receiverCount). Conditions belong to this lock.
    private final ReentrantLock stateLock = new ReentrantLock(true);
    private final Condition waitingReceivers = stateLock.newCondition();
    private final Condition waitingForAck = stateLock.newCondition();

    private final String name;
    private T value;
    private boolean hasValue = false;
    private int receiverCount = 0;

    private final List<Observer> inObservers = new ArrayList<>();
    private final List<Observer> outObservers = new ArrayList<>();

    public Channel(String name) {
        this.name = name;
    }

    public void out(T v) {
        senderLock.lock();
        try {
            List<Observer> toNotify;

            stateLock.lock();
            try {
                boolean interrupted = false;
                while (hasValue) {
                    try {
                        waitingForAck.await();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                value = v;
                hasValue = true;
                toNotify = new ArrayList<>(outObservers);
                outObservers.clear();
                waitingReceivers.signal();
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                stateLock.unlock();
            }

            // Fire observers outside both locks. Selectors' update() may take
            // their own monitor; firing under stateLock would risk deadlock,
            // and firing under senderLock would needlessly block other ops.
            for (Observer observer : toNotify) {
                observer.update();
            }

            stateLock.lock();
            try {
                boolean interrupted = false;
                while (hasValue) {
                    try {
                        waitingForAck.await();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                stateLock.unlock();
            }
        } finally {
            senderLock.unlock();
        }
    }

    public T in() {
        List<Observer> toNotify = new ArrayList<>();
        T res = null;

        stateLock.lock();
        try {
            receiverCount++;
            // Only fire in-observers when this in() will actually wait: if
            // hasValue is already true we consume immediately, so there is no
            // real "receiver is waiting" opportunity to announce. Firing
            // anyway would wake intent-out selectors and cause spurious extra
            // out() calls that have no matching consumer.
            if (!hasValue) {
                toNotify.addAll(inObservers);
                inObservers.clear();
            }
        } finally {
            stateLock.unlock();
        }

        for (Observer observer : toNotify) {
            observer.update();
        }

        stateLock.lock();
        try {
            boolean interrupted = false;
            while (!hasValue) {
                try {
                    waitingReceivers.await();
                } catch (InterruptedException e) {
                    interrupted = true;
                    waitingReceivers.signal();
                }
            }
            res = value;
            value = null;
            hasValue = false;
            waitingForAck.signal();
            
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            receiverCount--;
            stateLock.unlock();
        }

        return res;
    }

    public String getName() {
        return name;
    }

    public void observe(Direction direction, Observer observer) {
        boolean fireNow = false;
        stateLock.lock();
        try {
            if (direction == Direction.In && receiverCount > 0 && !hasValue) {
                // A receiver is actually blocked waiting for a value (no
                // value in transit yet): a real opportunity for an intent-out
                // user. If hasValue is true the receiver would just consume
                // and leave, so there is no opportunity to announce.
                fireNow = true;
            } else if (direction == Direction.Out && hasValue) {
                fireNow = true;
            } else if (direction == Direction.In) {
                inObservers.add(observer);
            } else {
                outObservers.add(observer);
            }
        } finally {
            stateLock.unlock();
        }
        // Fire outside the lock: the observer's update() may acquire other
        // monitors (e.g. the Selector's), and we must not invite a deadlock.
        if (fireNow) {
            observer.update();
        }
    }

    @Override
    public void unobserve(Direction direction, Observer observer) {
        stateLock.lock();
        try {
            if (direction == Direction.In) {
                inObservers.remove(observer);
            } else {
                outObservers.remove(observer);
            }
        } finally {
            stateLock.unlock();
        }
    }

    // Snapshot non bloquant de la disponibilité du canal pour la direction
    // demandée. Mêmes conditions que le déclenchement immédiat dans observe() :
    // - In  : un in() peut consommer tout de suite s'il y a une valeur en transit
    // - Out : un out() peut s'effectuer s'il y a un lecteur en attente et le slot libre
    @Override
    public boolean ready(Direction dir) {
        stateLock.lock();
        try {
            return switch (dir) {
                case In  -> hasValue;
                case Out -> receiverCount > 0 && !hasValue;
            };
        } finally {
            stateLock.unlock();
        }
    }
}
