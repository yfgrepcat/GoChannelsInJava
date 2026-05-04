/*
** @author philippe.queinnec@enseeiht.fr
** Inspired by IBM TSpaces exemples.
**
**/

package go.whiteboard;

import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import go.*;

/**
 ** The model of the whiteboard.
 **
 */
public class WhiteboardModel {

    /** The graphic part of the whiteboard. */
    private WhiteboardView view;

    private Channel<ColoredShape> inDraw;
    private Channel<Boolean> inEraseAll;
    private Channel<Integer> inRotate;
    private Channel<ColoredShape> serverDraw;
    private Channel<Boolean> serverEraseAll;
    private Channel<Integer> serverRotate;

    /** The lines and their respective colors that this client knows about. */
    private Set<ColoredShape> lines = new HashSet<>();

    private boolean eraseFlag = false;   // set true when erase command received

    public WhiteboardModel() {
    }
    
    public void setView(WhiteboardView view) {
        this.view = view;
    }

    public Set<ColoredShape> getLines() {
        return lines;
    }

    public void start() {
        Factory factory = new go.cs.Factory();
        String name = "" + ((new java.util.Random()).nextInt(10000));
        Channel<WhiteboardClient> connect = factory.newChannel("connect");
        inDraw = factory.newChannel("draw" + name);
        inEraseAll = factory.newChannel("erase" + name);
        inRotate = factory.newChannel("rotate" + name);
        serverDraw = factory.newChannel("draw");
        serverEraseAll = factory.newChannel("eraseAll");
        serverRotate = factory.newChannel("rotate");
        new Thread(new Callback(factory)).start();
        connect.out(new WhiteboardClient(inDraw, inEraseAll, inRotate));
    }
    
    /**
     ** This is called when the windowClosing event arrives.
     */
    public void terminate() {
        // XXXX TODO inform server to remove this client's channels
        System.exit(0);
    }

    /** Global Erase of the whiteboard.
     */
    public void eraseAll() {
        System.out.println("Erase all");
        serverEraseAll.out(true);
    }	
    
    /** Rotate all the shapes. */
    public void rotateAll(int degree) {
        System.out.println("Rotate");
        serverRotate.out(degree);
    }

    /** Request an exclusive access to the whiteboard.
     * Block until it has succeeded.
     */
    public void acquireExclusiveAccess() {
        System.err.println("Exclusive access: not implemented");
    }

    /** Release the exclusive access. */
    public void releaseExclusiveAccess() {
        System.err.println("Exclusive access: not implemented");
    }

    /**
     ** Publish a new shape (line or point)
     */
    public void addShape (ColoredShape shape)
    {
        serverDraw.out(shape);
    }

    /***************************************************************/
    
    private class Callback implements Runnable {
        private Factory factory;
        public Callback(Factory factory) { this.factory = factory; }
        public void run() {
            Selector selector = factory.newSelector(java.util.Set.of(inDraw, inEraseAll, inRotate), Direction.In);
            while (true) {
                Channel c = selector.select();
                if (c.equals(inDraw)) {
                    ColoredShape shape = (ColoredShape) inDraw.in();
                    System.out.println("Draw from server");
                    lines.add(shape);
                    view.redraw();
                } else if (c.equals(inEraseAll)) {
                    Boolean b = (Boolean) inEraseAll.in();
                    System.out.println("Erase from server");
                    lines.clear();
                    view.setClear();
                    view.redraw();
                } else if (c.equals(inRotate)) {
                    Integer angle = (Integer) inRotate.in();
                    System.out.println("Rotate from server");
                    // Let's be careful: rotation with center in WIDTH/2, HEIGHT/2
                    AffineTransform at = new AffineTransform();
                    at.rotate(Math.toRadians(angle), view.drawing.getSize().width / 2.0, view.drawing.getSize().height / 2.0);
                    //at.quadrantRotate(1, view.drawing.getSize().width / 2.0, view.drawing.getSize().height / 2.0);
                    for (ColoredShape rc : lines) {
                        rc.shape = at.createTransformedShape(rc.shape);
                    }
                    view.setClear();
                    view.redraw();
                } else {
                    throw new RuntimeException("Selector returns non-selected channel");
                }
            }
        }
    }

}

