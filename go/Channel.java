package go;

/** Un canal de communication synchrone, qui permet d'envoyer et recevoir des messages de type T. */
public interface Channel<T> extends java.io.Serializable {

    /** Envoi synchrone d'un message.
     * Bloque tant que le message ne peut pas être envoyé. */
    public void out(T v);
    
    /** Réception synchrone d'un message.
     * Bloque tant qu'il n'y a pas de message disponible. */
    public T in();

    /** Nom du canal.
     * On suppose que les noms sont uniques (en particulier pour client-serveur ou point-à-point).
     */
    public String getName();

    /** Ajoute un observateur à l'ensemble des observateurs pour ce canal,
     * pour la direction spécifiée. L'observateur est notifié lorsqu'une
     * opération in/out est en attente, puis il est retiré : une observation
     * ne se déclenche qu'une seule fois. Si une opération in/out est
     * déjà en attente au moment de l'appel à observe, l'observation se
     * déclenche immédiatement. */
    public void observe(Direction direction, Observer observer);

}
