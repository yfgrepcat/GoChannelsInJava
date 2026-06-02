package go.test;

import go.Direction;
import go.*;

/* select mixte  */
public class TestCS13 {

    private static void quit(String msg) {
        System.out.println("TestCS13: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        Factory factory = new go.cs.Factory();
        Channel<Integer> c1 = factory.newChannel("c1_test_13");
        Channel<Integer> c2 = factory.newChannel("c2_test_13");
        Channel<Integer> c3 = factory.newChannel("c3_test_13");

        Selector s = factory.newSelector(java.util.Map.of(c1, Direction.Out,
                c2, Direction.Out,
                c3, Direction.In));

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            quit("KO (deadlock)");
        }).start();

        new Thread(() -> {
            int v = c1.in();
            if (v != 4)
                quit("KO");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            c3.out(8);
            v = c2.in();
            if (v != 6)
                quit("KO");
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            @SuppressWarnings("unchecked")
            Channel<Integer> c = s.select();
            c.out(4);
            @SuppressWarnings("unchecked")
            Channel<Integer> cc = s.select();
            int v = cc.in();
            if (v != 8)
                quit("KO");
            @SuppressWarnings("unchecked")
            Channel<Integer> ccc = s.select();
            ccc.out(6);
            quit("ok");
        }).start();
    }
}
