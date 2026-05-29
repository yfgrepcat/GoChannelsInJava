package go.shm;

import go.Direction;
import go.Observer;
import go.Channel;

import java.util.HashMap;
import java.util.Map;

public class Selector implements go.Selector {

    private final Map<Channel, Direction> watchedChannels;

    public Selector(Map<Channel, Direction> channels) {
        this.watchedChannels = new HashMap<>(channels);
    }

    public Channel select() {
        // Active gates observer callbacks. Any late observer (still in a channel list
        // because we haven't unobserved it yet, but already drained by an
        // in()/out() in another thread) must not poison readyChannel (error on select().)
        final boolean[] active = { true };
        final Channel[] localReadyChannel = { null };
        final Object lock = new Object();
        Map<Channel, Observer> registeredObservers = new HashMap<>();

        // Fast path: si un canal est déjà servissable, on le prend tout de suite
        // sans enregistrer d'observateur. ready() est un snapshot non bloquant.
        for (Map.Entry<Channel, Direction> entry : watchedChannels.entrySet()) {
            Channel channel = entry.getKey();
            Direction direction = entry.getValue();
            if (((go.ChannelInterfaceDev<?>) channel).ready(direction)) {
                return channel;
            }
        }

        try {
            for (Map.Entry<Channel, Direction> entry : watchedChannels.entrySet()) {
                Channel channel = entry.getKey();
                Direction direction = entry.getValue();
                // To do an in() on this channel we observe its Out events (someone has put a value) 
                // to do an out() we observe its In events (a receiver is waiting).
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;

                Observer customObserver = new Observer() {
                    @Override
                    public void update() {
                        synchronized (lock) {
                            if (active[0] && localReadyChannel[0] == null) {
                                localReadyChannel[0] = channel;
                                lock.notify();
                            }
                        }
                    }
                };
                registeredObservers.put(channel, customObserver);
                channel.observe(observeDir, customObserver);
            }

            synchronized (lock) {
                boolean interrupted = false;
                while (localReadyChannel[0] == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                    }
                }
            } finally {
                synchronized (lock) {
                    active[0] = false;
                }
                for (Map.Entry<Channel, Observer> entry : registeredObservers.entrySet()) {
                    Channel channel = entry.getKey();
                    Observer observer = entry.getValue();
                    Direction direction = watchedChannels.get(channel);
                    Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;
                    ((go.ChannelInterfaceDev<?>) channel).unobserve(observeDir, observer);
                }
            }
        return localReadyChannel[0];
    }
}
