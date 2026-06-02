package go.test;

import go.Channel;
import go.Factory;

/** Deuxième site : se rattache au canal c (devient esclave) et envoie 7. */
public class TestSock03b {

    private static void quit(String msg) {
        System.out.println("TestSock03b: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.sock.Factory();
        Channel<Integer> c = factory.newChannel("sock03");
        c.out(7);
        quit("ok");
    }
}
