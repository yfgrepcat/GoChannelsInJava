package go.test;

import go.Channel;
import go.Direction;
import go.Factory;
import go.Selector;

import java.util.Set;

public class TestFactory {

    private static void quit(String msg) {
        System.out.println("TestFactory: " + msg);
        System.exit(msg.equals("ok") ? 0 : 1);
    }

    public static void main(String[] a) {
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            quit("KO (deadlock)");
        }).start();

        Factory factory = new go.shm.Factory();

        testSameNameSameChannel(factory);
        testDistinctNamesAreIndependent(factory);
        testSelectorAcceptsChannels(factory);

        quit("ok");
    }

    private static void testSameNameSameChannel(Factory factory) {
        Channel<Integer> c1 = factory.newChannel("partage");
        Channel<Integer> c2 = factory.newChannel("partage");
        if (c1 != c2) {
            quit("KO (même nom mais canaux différents)");
        }
        if (!"partage".equals(c1.getName())) {
            quit("KO (nom du canal incorrect : " + c1.getName() + ")");
        }
    }

    private static void testDistinctNamesAreIndependent(Factory factory) {
        Channel<Integer> a = factory.newChannel("alpha");
        Channel<Integer> b = factory.newChannel("beta");
        if (a == b) {
            quit("KO (noms distincts mais même canal)");
        }

        // On envoie sur a et on lit sur a : b ne doit pas interférer.
        new Thread(() -> a.out(1)).start();
        new Thread(() -> b.out(2)).start();

        int va = a.in();
        int vb = b.in();
        if (va != 1 || vb != 2) {
            quit("KO (canaux non indépendants : a=" + va + ", b=" + vb + ")");
        }
    }

    private static void testSelectorAcceptsChannels(Factory factory) {
        Channel<Integer> c = factory.newChannel("sel");
        Selector s = factory.newSelector(Set.of(c), Direction.In);

        new Thread(() -> c.out(5)).start();

        @SuppressWarnings("unchecked")
        Channel<Integer> ready = s.select();
        if (ready != c) {
            quit("KO (sélecteur : mauvais canal rendu)");
        }
        int v = ready.in();
        if (v != 5) {
            quit("KO (sélecteur : valeur " + v + " au lieu de 5)");
        }
    }
}
