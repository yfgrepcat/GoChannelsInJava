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
        for (Map.Entry<Channel, Direction> entry : watchedChannels.entrySet()) {
            Channel channel = entry.getKey();
            Direction direction = entry.getValue();

            // Register an observer for the channel and direction, which will set readyChannel when the channel is ready.
            Observer customObserver = new Observer() {
                @Override
                public void update() {
                    synchronized(Selector.this) {
                        if (readyChannel == null) {
                            readyChannel = channel;
                            Selector.this.notify(); // Notify the select method that a channel is ready.
                        }
                    }
                }
            };
            registeredObservers.put(channel, customObserver);
            channel.observe(direction, customObserver);
        }

        while (readyChannel == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return readyChannel;
    }

}
