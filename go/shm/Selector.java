package go.shm;

import go.Direction;
import go.Observer;
import go.Channel;

import java.util.HashMap;
import java.util.Map;

public class Selector implements go.Selector {

    private final Map<Channel, Direction> watchedChannels;
    private Channel readyChannel = null;

    public Selector(Map<Channel, Direction> channels) {
        this.watchedChannels = new HashMap<>(channels);
    }

    public Channel select() {
        // Active gates observer callbacks. Any late observer (still in a channel list
        // because we haven't unobserved it yet, but already drained by an
        // in()/out() in another thread) must not poison readyChannel (error on select().)
        final boolean[] active = { true };
        readyChannel = null;
        Map<Channel, Observer> registeredObservers = new HashMap<>();

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
                        synchronized (Selector.this) {
                            if (active[0] && readyChannel == null) {
                                readyChannel = channel;
                                Selector.this.notify();
                            }
                        }
                    }
                };
                registeredObservers.put(channel, customObserver);
                channel.observe(observeDir, customObserver);
            }

            synchronized (this) {
                while (readyChannel == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } finally {
            synchronized (this) {
                active[0] = false;
            }
            for (Map.Entry<Channel, Observer> entry : registeredObservers.entrySet()) {
                Channel channel = entry.getKey();
                Observer observer = entry.getValue();
                Direction direction = watchedChannels.get(channel);
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;
                ((go.shm.Channel) channel).unobserve(observeDir, observer);
            }
        }
        return readyChannel;
    }
}
