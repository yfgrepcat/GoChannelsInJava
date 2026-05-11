package go.cs;

import go.ChannelCS;

public class ObserverImpl implements go.Observer {

    private ChannelCS canal;
    private Selector selecteur;
    private boolean actif = true;

    public ObserverImpl(ChannelCS canal, Selector selecteur) {
        this.canal = canal;
        this.selecteur = selecteur;
    }

    public void desactiver() {
        this.actif = false;
    }

    // déclenche le sélecteur
    public void update() {
        if (actif) {
            selecteur.signale(canal);
        }

    }
}
