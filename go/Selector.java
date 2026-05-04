package go;

/** Attente de la possibilité de réaliser une action parmi un ensemble de canaux.
 * Une action est in() ou out(), un mix est possible.
 * Noter que les canaux peuvent transporter des types différents.
 */
public interface Selector {

    /** Renvoie un canal sur lequel l'action écoutée est faisable.
     * Si plusieurs canaux sont envisageables, l'un, indéterminé, est renvoyé.
     * Bloque tant qu'aucune action n'est faisable. */
    public Channel select();

}
