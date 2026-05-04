package go;

/** A class can implement the Observer interface when it wants to be informed of changes in a channel. */
@FunctionalInterface
public interface Observer {

    /** This method is called whenever the state of the observed channel is changed. */
    public void update();
}
