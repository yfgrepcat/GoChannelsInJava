package go;

import java.util.Set;
import java.util.Map;

/** Interface pour obtenir un canal ou un sélecteur.
 * Chaque implantation de canaux/sélecteurs fournit son implantation.
 * Il ne doit exister qu'une unique Factory par JVM (machine Java).
 */
public interface Factory {

    /** Création ou accès à un canal existant. */
    public <T> go.Channel<T> newChannel(String name);
    
    /** Spécifie quels sont les canaux écoutés et la direction pour chacun. */
    public Selector newSelector(Map<Channel, Direction> channels);

    /** Spécifie quels sont les canaux écoutés et la même direction pour tous. */
    public Selector newSelector(Set<Channel> channels, Direction direction);

}

