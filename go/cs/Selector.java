package go.cs;

import go.Direction;
import go.Channel;

import java.util.HashMap;
import java.util.Map;
import go.Observer;
import java.util.ArrayList;

public class Selector implements go.Selector {

    ChannelCS declencheur = null;
    private Map<ChannelCS, Direction> channels;

    public Selector(Map<ChannelCS, Direction> channels) {
        this.channels = channels;
    }

    // garantit le choix arbitraire entre plusieurs
    public synchronized void signale(ChannelCS canal) {
        if (this.declencheur == null) {
            this.declencheur = canal;
            notifyAll();
        }
    }

    // fait le tour des canaux + pose un observer sur chaque canal
    public Channel select() {

        List<ObserverImpl> observateurs = new ArrayList<>();
        declencheur = null;

        synchronized (this) {
            for (Map.Entry<ChannelCS, Direction> entry : channels.entrySet()) {
                ChannelCS canal = entry.getKey();
                Direction direction = entry.getValue();

                // création de l'observer
                ObserverImpl obs = new ObserverImpl(canal, this);
                observateurs.add(obs);

                try {
                    canal.observe(direction, obs);
                } catch (Exception e) {
                }
            }

            // un canal nous réveillera
            while (declencheur == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }

        }

        for (ObserverImpl obs : observateurs) {
            obs.desactiver();
        }

        return declencheur;
    }

}
