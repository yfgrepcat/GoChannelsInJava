/*
** @author philippe.queinnec@enseeiht.fr
** Inspired by IBM TSpaces exemples.
**
**/

package go.whiteboard;

public class Whiteboard {

    /*** main **
     ** Run the whiteboard as an application.
     **
     ** @param args - command line arguments
     */
    public static void main(String args[]) {
        WhiteboardModel model = new WhiteboardModel();
        WhiteboardView view = new WhiteboardView(model);
        model.setView(view);
        model.start();
    }
}

