package go.test;

import go.Direction;
import go.*;

/* select in */
public class TestCS11 {

    private static void quit(String msg) {
        System.out.println("TestCS11: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        Factory factory = new go.cs.Factory();
        Channel<Integer> c1 = factory.newChannel("c1_test_11");
        Channel<Integer> c2 = factory.newChannel("c2_test_11");
        Channel<Integer> c3 = factory.newChannel("c3_test_11");

        Selector s = factory.newSelector(java.util.Set.of(c1, c2, c3), Direction.In);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            quit("KO (deadlock)");
        }).start();

        new Thread(() -> {
            c1.out(4);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            c2.out(6);
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            @SuppressWarnings("unchecked")
            Channel<Integer> c = s.select();
            int v = c.in();
            if (v != 4)
                quit("KO");

            @SuppressWarnings("unchecked")
            Channel<Integer> cc = s.select();
            v = cc.in();
            if (v != 6)
                quit("KO");

            quit("ok");
        }).start();
    }
}
