package go;

/** Extension interne de Channel pour les besoins de l'implémentation (Selector).
 * N'altère pas l'API publique go.Channel : c'est une sous-interface dédiée aux
 * méthodes additionnelles utilisées en interne. */
public interface ChannelInterfaceDev<T> extends Channel<T> {

    /** Retire un observateur précédemment ajouté via observe(). */
    void unobserve(Direction dir, Observer observer);

    /** Snapshot non bloquant : l'opération `dir` peut-elle se faire sans bloquer ? */
    boolean ready(Direction dir);
}
