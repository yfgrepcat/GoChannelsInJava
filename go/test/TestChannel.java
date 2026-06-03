package go.test;

import go.Channel;
import go.Direction;
import go.Factory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class TestChannel {

    private static void quit(String msg) {
        System.out.println("TestChannel: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.shm.Factory();

        testGetName(factory);
        testRendezVous(factory);
        testObserveIn(factory);
        testObserveOut(factory);

        quit("ok");
    }

    private static void testGetName(Factory factory) {
        Channel<Integer> c = factory.newChannel("test");
        if (!"test".equals(c.getName())) {
            quit("KO (getName attendu \"test\", obtenu \"" + c.getName() + "\")");
        }
    }

    private static void testRendezVous(Factory factory) {
        Channel<Integer> c = factory.newChannel("rendezvous");
        AtomicBoolean sent = new AtomicBoolean(false);

        Thread sender = new Thread(() -> {
            c.out(42);
            sent.set(true);
        });
        sender.start();

        try { Thread.sleep(150); } catch (InterruptedException e) { }
        if (sent.get()) {
            quit("KO (out() a rendu la main avant tout in())");
        }

        int v = c.in();
        if (v != 42) {
            quit("KO (rendez-vous : valeur " + v + " au lieu de 42)");
        }

        try { sender.join(500); } catch (InterruptedException e) { }
        if (!sent.get()) {
            quit("KO (out() n'a pas rendu la main après consommation)");
        }
    }

    private static void testObserveIn(Factory factory) {
        Channel<Integer> c = factory.newChannel("observe-in");
        AtomicInteger fired = new AtomicInteger(0);

        // Un lecteur se met en attente (aucune valeur disponible) : il y a donc
        // une réelle opportunité d'émission à signaler.
        new Thread(() -> {
            int v = c.in();
            if (v != 7) quit("KO (observe-in : valeur " + v + " au lieu de 7)");
        }).start();

        try { Thread.sleep(100); } catch (InterruptedException e) { }

        c.observe(Direction.In, fired::incrementAndGet);

        // L'observation doit se déclencher (immédiatement, car un in() attend).
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        if (fired.get() != 1) {
            quit("KO (observe-in non déclenché : " + fired.get() + ")");
        }

        // On débloque le lecteur en attente pour ne pas laisser de thread pendu.
        c.out(7);
    }

    private static void testObserveOut(Factory factory) {
        Channel<Integer> c = factory.newChannel("observe-out");
        AtomicInteger fired = new AtomicInteger(0);

        // Un émetteur dépose une valeur ; elle reste en transit tant qu'aucun
        // lecteur ne l'a prise.
        new Thread(() -> c.out(9)).start();

        try { Thread.sleep(100); } catch (InterruptedException e) { }

        c.observe(Direction.Out, fired::incrementAndGet);

        try { Thread.sleep(100); } catch (InterruptedException e) { }
        if (fired.get() != 1) {
            quit("KO (observe-out non déclenché : " + fired.get() + ")");
        }

        int v = c.in();
        if (v != 9) {
            quit("KO (observe-out : valeur " + v + " au lieu de 9)");
        }
    }
}
