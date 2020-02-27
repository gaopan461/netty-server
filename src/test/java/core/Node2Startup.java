package core;

/**
 * @author gaopan
 */
public class Node2Startup {

    public static void main(String[] args) throws InterruptedException {
        Node node = new Node("node2", 10012);
        node.startup();
        while (true) {
            node.pulse();
            Thread.sleep(1000);
        }
    }

}
