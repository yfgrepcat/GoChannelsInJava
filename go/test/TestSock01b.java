package go.test;

import go.Channel;
import go.Factory;

/** Deuxième site : se rattache au canal c via le service de nommage
 *  (devient esclave) et reçoit la valeur. */
public class TestSock01b {

    private static void quit(String msg) {
        System.out.println("TestSock01b: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.sock.Factory();
        Channel<Integer> c = factory.newChannel("sock01");
        int v = c.in();
        quit(v == 4 ? "ok" : "KO (reçu " + v + ")");
    }
}
