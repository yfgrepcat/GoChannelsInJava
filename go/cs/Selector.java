package go.cs;

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
        final boolean[] active = { true };
        final Channel[] localReadyChannel = { null };
        final Object lock = new Object();
        Map<Channel, Observer> registeredObservers = new HashMap<>();

        try {
            for (Map.Entry<Channel, Direction> entry : watchedChannels.entrySet()) {
                Channel channel = entry.getKey();
                Direction direction = entry.getValue();
                
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;

                /* wrapper */
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
            // on clean le tout 
            for (Map.Entry<Channel, Observer> entry : registeredObservers.entrySet()) {
                Channel channel = entry.getKey();
                Observer observer = entry.getValue();
                Direction direction = watchedChannels.get(channel);
                Direction observeDir = (direction == Direction.In) ? Direction.Out : Direction.In;
                
                // on cast sur notre classe Channel pour appeler unobserve
                ((go.cs.Channel) channel).unobserve(observeDir, observer);
            }
        }
        
        return localReadyChannel[0];
    }
}
