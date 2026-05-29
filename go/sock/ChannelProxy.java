package go.sock;

import java.io.Serializable;

/** Substitut sérialisable pour un go.sock.Channel{Master,Slave}.
 *  Quand un canal voyage à travers un autre canal, on n'envoie pas l'objet
 *  réel (qui contient un ServerSocket ou un canal shm) : on envoie juste son
 *  nom. À la réception, readResolve reconstruit (via la Factory locale et le
 *  service de nommage) le bon Master ou Slave selon le site.
 */
final class ChannelProxy implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    ChannelProxy(String name) {
        this.name = name;
    }

    private Object readResolve() {
        Factory factory = Factory.current();
        if (factory == null) {
            throw new IllegalStateException(
                "Aucune go.sock.Factory dans cette JVM : impossible de résoudre " + name);
        }
        return factory.newChannel(name);
    }
}
