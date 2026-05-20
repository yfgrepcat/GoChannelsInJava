import go.*;

public class TestVide {
    public static void main(String[] a) throws Exception {
        Factory factory = new go.shm.Factory();
        Channel<Integer> channel = factory.newChannel("a");
        System.out.println("");
        System.out.println("Ok, I have created a channel.");
    }
}
