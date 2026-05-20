package go.shm;

import go.Direction;
import go.Observer;
import go.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Selector implements go.Selector {

    private final Map<Channel, Direction> watchedChannels;
    private Channel readyChannel = null;

    public Selector(Map<Channel, Direction> channels) {
        this.watchedChannels = new HashMap<>(channels);
    }

    public Channel select() {
        readyChannel = null;
        Map<Channel, Observer> registeredObservers = new HashMap<>();

        try{
            for (Map.Entry<Channel, Direction> entry : watchedChannels.entrySet()) {
                Channel channel = entry.getKey();
                Direction direction = entry.getValue();
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;

                // Register an observer for the channel and direction, which will set
                // readyChannel when the channel is ready.
                Observer customObserver = new Observer() {
                    @Override
                    public void update() {
                        synchronized (Selector.this) {
                            if (readyChannel == null) {
                                readyChannel = channel;
                                Selector.this.notify(); // Notify the select method that a channel is ready.
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
            // Unregister all observers to avoid memory leaks and unintended notifications.
            for (Map.Entry<Channel, Observer> entry : registeredObservers.entrySet()) {
                Channel channel = entry.getKey();
                Observer observer = entry.getValue();
                Direction direction = watchedChannels.get(channel);
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;
                channel.unobserve(observeDir, observer);
            }
        }
    return readyChannel;
    }
}