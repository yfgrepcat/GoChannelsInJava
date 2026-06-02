package go.test;

import go.Channel;
import go.Direction;
import go.Factory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Tests unitaires du contrat go.Channel (implantation shm), pour les aspects
 *  que les scénarios numérotés (TestShm01/03/...) n'exercent pas directement :
 *   - getName() rend bien le nom passé à la fabrique ;
 *   - out() est un rendez-vous synchrone : il ne rend la main qu'une fois la
 *     valeur effectivement consommée par un in() ;
 *   - observe(Direction.In) se déclenche quand un in() est en attente ;
 *   - observe(Direction.Out) se déclenche quand une valeur est en transit.
 */
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

    /** getName() doit rendre exactement le nom fourni à newChannel. */
    private static void testGetName(Factory factory) {
        Channel<Integer> c = factory.newChannel("baptise");
        if (!"baptise".equals(c.getName())) {
            quit("KO (getName attendu \"baptise\", obtenu \"" + c.getName() + "\")");
        }
    }

    /** out() ne doit rendre la main qu'après consommation par in() : on vérifie
     *  que l'émetteur reste bloqué tant qu'aucun lecteur n'a pris la valeur. */
    private static void testRendezVous(Factory factory) {
        Channel<Integer> c = factory.newChannel("rendezvous");
        AtomicBoolean sent = new AtomicBoolean(false);

        Thread sender = new Thread(() -> {
            c.out(42);
            sent.set(true);
        });
        sender.start();

        // Laisse le temps à l'émetteur de déposer la valeur ; comme personne
        // n'a encore lu, out() doit toujours être bloqué.
        try { Thread.sleep(150); } catch (InterruptedException e) { }
        if (sent.get()) {
            quit("KO (out() a rendu la main avant tout in())");
        }

        int v = c.in();
        if (v != 42) {
            quit("KO (rendez-vous : valeur " + v + " au lieu de 42)");
        }

        // Une fois la valeur consommée, out() doit se débloquer rapidement.
        try { sender.join(500); } catch (InterruptedException e) { }
        if (!sent.get()) {
            quit("KO (out() n'a pas rendu la main après consommation)");
        }
    }

    /** Un observateur en direction In doit être notifié lorsqu'un in() est en
     *  attente sur le canal (opportunité d'émettre). */
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

    /** Un observateur en direction Out doit être notifié lorsqu'une valeur est
     *  en transit sur le canal (opportunité de recevoir). */
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

        // On consomme la valeur pour libérer l'émetteur.
        int v = c.in();
        if (v != 9) {
            quit("KO (observe-out : valeur " + v + " au lieu de 9)");
        }
    }
}
