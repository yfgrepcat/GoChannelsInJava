package go.whiteboard;

import go.*;
import java.util.*;

public class WhiteboardServer {

    private Channel<Channel<ColoredShape>> connect;
    private Channel<ColoredShape> draw;
    private Channel<Boolean> eraseAll;
    private Channel<Integer> rotate;

    private Set<WhiteboardClient> clients = new HashSet<>();

    private Set<ColoredShape> shapes = new HashSet<>();

    private void main() {
        Factory factory = new go.cs.Factory();
        connect = factory.newChannel("connect");
        draw = factory.newChannel("draw");
        eraseAll = factory.newChannel("eraseAll");
        rotate = factory.newChannel("rotate");
        Selector selector = factory.newSelector(java.util.Set.of(connect, draw, eraseAll, rotate), Direction.In);
        while (true) {
            Channel c = selector.select();
            if (c.equals(connect)) {
                @SuppressWarnings("unchecked")
                WhiteboardClient client = (WhiteboardClient) c.in();
                System.out.println("new client " + client.draw.getName());
                clients.add(client);
                for (var s : shapes) client.draw.out(s);
            } else if (c.equals(draw)) {
                ColoredShape s = (ColoredShape) c.in();
                shapes.add(s);
                for (var cli : clients) cli.draw.out(s);
            } else if (c.equals(eraseAll)) {
                Boolean b = (Boolean) c.in(); // unused
                shapes.clear();
                for (var cli : clients) cli.eraseAll.out(true);
            } else if (c.equals(rotate)) {
                int angle = (Integer) c.in();
                // XXXX TODO update the shapes (for the future clients)
                for (var cli : clients) cli.rotate.out(angle);
            } else {
                throw new RuntimeException("Selector returns non-selected channel");
            }
        }
    }
        
    
    public static void main(String args[]) {
        (new WhiteboardServer()).main();
    }
}
