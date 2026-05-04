package go.whiteboard;

import go.Channel;

public class WhiteboardClient implements java.io.Serializable {
    public Channel<ColoredShape> draw;
    public Channel<Boolean> eraseAll;
    public Channel<Integer> rotate;

    public WhiteboardClient(Channel<ColoredShape> draw, Channel<Boolean> eraseAll, Channel<Integer> rotate) {
        this.draw = draw;
        this.eraseAll = eraseAll;
        this.rotate = rotate;
    }
}
