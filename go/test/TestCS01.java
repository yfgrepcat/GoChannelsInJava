package go.test;

import go.Channel;
import go.Factory;

/** in/out, start with in */
public class TestCS01 {

    private static void quit(String msg) {
        System.out.println("TestCS01: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        Factory factory = new go.cs.Factory();
        Channel<Integer> c = factory.newChannel("c_test_01");

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            quit("KO (deadlock)");
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            c.out(4);
        }).start();

        new Thread(() -> {
            int v = c.in();
            quit(v == 4 ? "ok" : "KO");
        }).start();
    }
}
