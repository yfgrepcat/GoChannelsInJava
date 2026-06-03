package go.test;

import go.Channel;
import go.Direction;
import go.Factory;
import go.Selector;

import java.util.HashMap;
import java.util.Map;

public class TestSelector {

    private static void quit(String msg) {
        System.out.println("TestShmSelector: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        Factory factory = new go.shm.Factory();
        Channel<Integer> c1 = factory.newChannel("c1");
        Channel<Integer> c2 = factory.newChannel("c2");

        // Simulation d'un environnement concurrent et asynchrone
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            c1.out(40);
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            c2.out(84);
        }).start();

        // Canaux à observer
        Map<Channel, Direction> channelsToObserve = new HashMap<>();
        channelsToObserve.put(c1, Direction.In);
        channelsToObserve.put(c2, Direction.In);

        Selector selector = factory.newSelector(channelsToObserve);
        Channel ready = selector.select();

        if (ready != c1) {
            quit("KO (wrong channel selected, expected c1)");
        }

        int val1 = c1.in();
        if (val1 != 40)
            quit("KO (wrong value on c1)");

        ready = selector.select();
        if (ready != c2) {
            quit("KO (wrong channel selected, expected c2)");
        }

        int val2 = c2.in();
        if (val2 != 84)
            quit("KO (wrong value on c2)");

        quit("ok");
    }
}
