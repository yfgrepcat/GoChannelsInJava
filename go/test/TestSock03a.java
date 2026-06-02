package go.test;

import go.Channel;
import go.Factory;

/** Premier site : crée le canal c (devient maître) et attend une valeur.
 *  Variante inverse de TestSock01 — vérifie que le maître peut aussi
 *  être le receveur. */
public class TestSock03a {

    private static void quit(String msg) {
        System.out.println("TestSock03a: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.sock.Factory();
        Channel<Integer> c = factory.newChannel("sock03");
        int v = c.in();
        quit(v == 7 ? "ok" : "KO (reçu " + v + ")");
    }
}
