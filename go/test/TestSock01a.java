package go.test;

import go.Channel;
import go.Factory;

/** Premier site : crée le canal c (devient maître) et envoie 4. */
public class TestSock01a {

    private static void quit(String msg) {
        System.out.println("TestSock01a: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.sock.Factory();
        Channel<Integer> c = factory.newChannel("sock01");
        c.out(4);
        quit("ok");
    }
}
